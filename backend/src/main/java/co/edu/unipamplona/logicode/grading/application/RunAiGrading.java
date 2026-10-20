package co.edu.unipamplona.logicode.grading.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.grading.domain.AiEvaluation;
import co.edu.unipamplona.logicode.grading.domain.AiGradingJob;
import co.edu.unipamplona.logicode.grading.domain.AiGradingPolicy;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.GradeSource;
import co.edu.unipamplona.logicode.grading.domain.ReviewReason;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Worker de calificación con IA (RF-14, RF-15, RNF-06 a RNF-08). Toma un trabajo en una transacción corta, llama al
 * modelo fuera de toda transacción y guarda el resultado en otra. Solo la toma vigente cierra el trabajo y solo una
 * nota aún en evaluación cambia, así un reintento nunca duplica ni pisa una nota (por ejemplo, si el docente calificó
 * mientras tanto). Un fallo técnico no genera nota: se reintenta y, agotados los intentos, pasa al docente.
 */
@Service
public class RunAiGrading {

    private static final Logger log = LoggerFactory.getLogger(RunAiGrading.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private record Claim(UUID jobId, UUID token, UUID gradeId, int attempts) {
    }

    /** Lo necesario para evaluar, o la razón para no hacerlo. */
    private record Task(RubricVersion rubric, GradingProvider.Request request, boolean unreadable, boolean skip) {

        static Task ready(RubricVersion rubric, GradingProvider.Request request) {
            return new Task(rubric, request, false, false);
        }

        static Task unreadableContent() {
            return new Task(null, null, true, false);
        }

        static Task nothingToDo() {
            return new Task(null, null, false, true);
        }
    }

    private record Result(AiEvaluation evaluation, AiGradingPolicy.Decision decision, String error,
            boolean unreadable) {

        static Result failure(AiEvaluation evaluation, String error) {
            return new Result(evaluation, null, error, false);
        }
    }

    private final AiGradingJobRepository jobs;
    private final AiEvaluationRepository evaluations;
    private final GradeRepository grades;
    private final GradeChangeRepository changes;
    private final RubricRepository rubrics;
    private final ActivityCatalog catalog;
    private final AiSubmissionContent content;
    private final GradingProvider provider;
    private final AiGradingSettings settings;
    private final TransactionTemplate transaction;
    private final TransactionTemplate readOnly;
    private final Clock clock;

    public RunAiGrading(AiGradingJobRepository jobs, AiEvaluationRepository evaluations, GradeRepository grades,
            GradeChangeRepository changes, RubricRepository rubrics, ActivityCatalog catalog,
            AiSubmissionContent content, GradingProvider provider, AiGradingSettings settings,
            PlatformTransactionManager transactionManager, Clock clock) {
        this.jobs = jobs;
        this.evaluations = evaluations;
        this.grades = grades;
        this.changes = changes;
        this.rubrics = rubrics;
        this.catalog = catalog;
        this.content = content;
        this.provider = provider;
        this.settings = settings;
        this.transaction = new TransactionTemplate(transactionManager);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.clock = clock;
    }

    /** Procesa el siguiente trabajo listo de la cola. Devuelve {@code false} si no había ninguno. */
    public boolean processNext() {
        Claim claim = transaction.execute(status -> {
            Instant now = clock.instant();
            return jobs.lockNext(now, now.minus(settings.staleAfter())).map(job -> {
                UUID token = job.claim(now);
                jobs.save(job);
                return new Claim(job.getId(), token, job.getGradeId(), job.getAttempts());
            }).orElse(null);
        });
        if (claim == null) {
            return false;
        }
        Result result = evaluate(claim);
        for (int attempt = 1; ; attempt++) {
            try {
                transaction.executeWithoutResult(status -> finish(claim, result));
                return true;
            } catch (OptimisticLockingFailureException e) {
                if (attempt == 3) {
                    throw e;
                }
            }
        }
    }

    /** Con la IA desactivada, los trabajos que quedaron en cola pasan al docente. */
    public int releaseQueue() {
        Integer released = transaction.execute(status -> {
            Instant now = clock.instant();
            int count = 0;
            for (AiGradingJob job : jobs.findActive()) {
                job.cancel("La calificación con IA está desactivada", now);
                jobs.save(job);
                grades.findById(job.getGradeId()).filter(Grade::isProcessing).ifPresent(grade -> {
                    grade.sendToReview(ReviewReason.AI_DISABLED, now);
                    grades.save(grade);
                });
                count++;
            }
            return count;
        });
        return released == null ? 0 : released;
    }

    private Result evaluate(Claim claim) {
        Task task;
        try {
            task = readOnly.execute(status -> prepare(claim.gradeId()));
        } catch (RuntimeException e) {
            log.warn("No fue posible preparar la evaluación con IA del trabajo {}", claim.jobId(), e);
            return Result.failure(null, "No fue posible leer la entrega para evaluarla.");
        }
        if (task.skip()) {
            return new Result(null, null, null, false);
        }
        if (task.unreadable()) {
            return new Result(null, null, null, true);
        }
        UUID gradeId = claim.gradeId();
        UUID rubricId = task.rubric().getId();
        if (claim.attempts() > settings.maxAttempts()) {
            String error = "Se agotaron los intentos: la evaluación se interrumpió sin terminar.";
            return Result.failure(AiEvaluation.failed(gradeId, rubricId, provider.model(), error, 0, clock.instant()),
                    error);
        }
        long started = System.nanoTime();
        try {
            GradingProvider.Result answer = provider.evaluate(task.request());
            AiGradingPolicy.Decision decision = AiGradingPolicy.decide(task.rubric(), answer.legible(),
                    answer.confidence(), answer.criteria().stream()
                            .map(c -> new AiGradingPolicy.CriterionResult(c.points(), c.comment())).toList());
            AiEvaluation evaluation = AiEvaluation.completed(gradeId, rubricId, provider.model(), decision,
                    blankToNull(answer.transcription()), blankToNull(answer.feedback()), elapsedMs(started),
                    clock.instant());
            log.info("Evaluación con IA del trabajo {}: {} (confianza {}, {} ms)", claim.jobId(), decision.outcome(),
                    decision.confidence(), evaluation.getDurationMs());
            return new Result(evaluation, decision, null, false);
        } catch (GradingProvider.GradingFailure | IllegalArgumentException e) {
            log.warn("Falló la evaluación con IA del trabajo {} (intento {}): {}", claim.jobId(), claim.attempts(),
                    e.getMessage());
            String error = truncate(e.getMessage());
            return Result.failure(AiEvaluation.failed(gradeId, rubricId, provider.model(), error, elapsedMs(started),
                    clock.instant()), error);
        } catch (RuntimeException e) {
            log.error("Error inesperado al evaluar con IA el trabajo {}", claim.jobId(), e);
            String error = "Error inesperado al evaluar la entrega.";
            return Result.failure(AiEvaluation.failed(gradeId, rubricId, provider.model(), error, elapsedMs(started),
                    clock.instant()), error);
        }
    }

    private Task prepare(UUID gradeId) {
        Optional<Grade> found = grades.findById(gradeId).filter(Grade::isProcessing);
        if (found.isEmpty()) {
            return Task.nothingToDo();
        }
        Grade grade = found.get();
        RubricVersion rubric = rubrics.findById(grade.getRubricVersionId()).orElseThrow();
        Optional<ActivityCatalog.SubmissionInfo> submission = catalog.submission(grade.getSubmissionId());
        Optional<AiSubmissionContent.Loaded> loaded = content.load(grade.getSubmissionId());
        if (submission.isEmpty() || loaded.isEmpty()) {
            return Task.unreadableContent();
        }
        GradingProvider.Request request = new GradingProvider.Request(submission.get().studentName(),
                submission.get().activityTitle(), rubric.getStatement(), rubric.getExpectedAnswer(),
                rubric.getCriteria().stream().map(c -> new GradingProvider.Criterion(c.getName(), c.getDescription(),
                        c.getMaxPoints())).toList(),
                loaded.get().textAnswer(), loaded.get().textFiles(), loaded.get().images());
        return Task.ready(rubric, request);
    }

    private void finish(Claim claim, Result result) {
        Optional<AiGradingJob> found = jobs.findById(claim.jobId()).filter(job -> job.isClaimedBy(claim.token()));
        if (found.isEmpty()) {
            log.warn("El trabajo {} ya no pertenece a esta toma; se descarta su resultado", claim.jobId());
            return;
        }
        AiGradingJob job = found.get();
        Instant now = clock.instant();
        Optional<Grade> processing = grades.findById(claim.gradeId()).filter(Grade::isProcessing);
        if (result.evaluation() != null) {
            evaluations.save(result.evaluation());
        }
        if (result.error() != null) {
            boolean retry = job.fail(result.error(), settings.maxAttempts(), settings.retryDelay(), now);
            jobs.save(job);
            if (!retry) {
                processing.ifPresent(grade -> {
                    grade.sendToReview(ReviewReason.AI_FAILED, now);
                    grades.save(grade);
                });
            }
            return;
        }
        job.succeed(now);
        jobs.save(job);
        if (processing.isEmpty()) {
            return;
        }
        Grade grade = processing.get();
        if (result.unreadable()) {
            grade.sendToReview(ReviewReason.UNSUPPORTED, now);
        } else if (result.decision().publishes()) {
            changes.save(grade.publish(result.decision().score(), result.evaluation().getFeedback(), GradeSource.AI,
                    null, now));
        } else {
            grade.sendToReview(result.decision().reviewReason(), now);
        }
        grades.save(grade);
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    private static String truncate(String message) {
        String text = message == null || message.isBlank() ? "Fallo técnico sin detalle." : message.strip();
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH) + "…";
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
