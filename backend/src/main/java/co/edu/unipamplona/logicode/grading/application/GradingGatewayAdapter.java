package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.GradingGateway;
import co.edu.unipamplona.logicode.grading.domain.AiGradingJob;
import co.edu.unipamplona.logicode.grading.domain.BlockChallenge;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.GradeChange;
import co.edu.unipamplona.logicode.grading.domain.GradeSource;
import co.edu.unipamplona.logicode.grading.domain.ReviewReason;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import co.edu.unipamplona.logicode.pseint.application.RunPrograms;
import org.springframework.stereotype.Component;

/**
 * Implementa el puerto de activities: rúbrica obligatoria, una nota por entrega (calificada con casos de prueba, en
 * cola de IA o en Por revisar) y versiones de rúbrica por cambio de enunciado.
 */
@Component
class GradingGatewayAdapter implements GradingGateway {

    private final RubricRepository rubrics;
    private final GradeRepository grades;
    private final AiGradingJobRepository jobs;
    private final AiSubmissionContent content;
    private final GradeChangeRepository changes;
    private final RunPrograms programs;
    private final AiGradingSettings settings;
    private final Clock clock;

    GradingGatewayAdapter(RubricRepository rubrics, GradeRepository grades, AiGradingJobRepository jobs,
            AiSubmissionContent content, GradeChangeRepository changes, RunPrograms programs,
            AiGradingSettings settings, Clock clock) {
        this.rubrics = rubrics;
        this.grades = grades;
        this.jobs = jobs;
        this.content = content;
        this.changes = changes;
        this.programs = programs;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public boolean hasRubric(UUID activityId) {
        return rubrics.findCurrent(activityId).isPresent();
    }

    /**
     * La entrega queda con la rúbrica vigente. Con casos de prueba y código PSeInt se califica en el acto ejecutándolo
     * (RF-16); si el programa no se puede leer, pasa al docente. Si no, entra a la cola de IA en la misma transacción
     * (RNF-07: nunca se pierde ni bloquea la entrega). Sin IA o con formatos que la IA no lee, pasa a Por revisar.
     */
    @Override
    public void submissionReceived(UUID submissionId, UUID activityId) {
        RubricVersion current = current(activityId);
        Instant now = clock.instant();
        Optional<String> code = current.hasTestCases() ? content.pseintCode(submissionId) : Optional.empty();
        if (current.getBlockChallenge() != null) {
            gradeBlocks(submissionId, current, now);
        } else if (code.isPresent()) {
            RunPrograms.TestReport report = programs.check(code.get(), current.getTestCases().stream()
                    .map(t -> new RunPrograms.TestCase(t.getInput(), t.getExpectedOutput())).toList());
            if (report.runnable()) {
                publishAutomatic(submissionId, current, report.score(), feedback(report), now);
            } else {
                grades.save(Grade.forReview(submissionId, current.getId(), ReviewReason.CODE_ERROR, now));
            }
        } else if (!settings.enabled()) {
            grades.save(Grade.forReview(submissionId, current.getId(), ReviewReason.AI_DISABLED, now));
        } else if (!content.isReadable(submissionId)) {
            grades.save(Grade.forReview(submissionId, current.getId(), ReviewReason.UNSUPPORTED, now));
        } else {
            Grade grade = grades.save(Grade.forAi(submissionId, current.getId(), now));
            jobs.save(new AiGradingJob(grade.getId(), null, now));
        }
    }

    /** RF-12: se vuelve a ejecutar el programa entregado; si no se puede leer o rompe las reglas, pasa al docente. */
    private void gradeBlocks(UUID submissionId, RubricVersion rubric, Instant now) {
        BlockChallenge.Result result;
        try {
            BlockChallenge challenge = BlockChallengeData.read(rubric.getBlockChallenge()).toDomain();
            result = challenge.run(BlockChallengeData.program(content.textAnswer(submissionId).orElse("")));
        } catch (IllegalArgumentException e) {
            grades.save(Grade.forReview(submissionId, rubric.getId(), ReviewReason.CODE_ERROR, now));
            return;
        }
        publishAutomatic(submissionId, rubric, result.score(), feedback(result), now);
    }

    static String feedback(BlockChallenge.Result result) {
        return switch (result.outcome()) {
            case GOAL -> "Reto superado: el robot llegó a la meta con " + result.blocks() + " bloques.";
            case CRASH -> "El robot chocó con un muro o con el borde del tablero.";
            case ENDED -> "El programa terminó, pero el robot no llegó a la meta.";
            case LIMIT -> "El robot dio más de " + BlockChallenge.MAX_STEPS + " pasos sin llegar: revisa los ciclos.";
        };
    }

    @Override
    public void submissionScored(UUID submissionId, UUID activityId, BigDecimal score, String feedback) {
        publishAutomatic(submissionId, current(activityId), score, feedback, clock.instant());
    }

    private void publishAutomatic(UUID submissionId, RubricVersion rubric, BigDecimal score, String feedback,
            Instant now) {
        Grade grade = new Grade(submissionId, rubric.getId(), now);
        GradeChange change = grade.publish(score, feedback, GradeSource.AUTOMATIC, null, now);
        grades.save(grade);
        changes.save(change);
    }

    /** Lo que ve el estudiante: cuántos casos pasaron y por qué falló cada uno, sin las entradas ni las salidas. */
    static String feedback(RunPrograms.TestReport report) {
        StringBuilder text = new StringBuilder("Casos de prueba aprobados: " + report.passed() + " de "
                + report.total() + ".");
        for (RunPrograms.CaseOutcome outcome : report.cases()) {
            text.append("\nCaso ").append(outcome.number()).append(": ");
            if (outcome.passed()) {
                text.append("correcto.");
            } else if (outcome.error() != null) {
                text.append("el programa se detuvo. ").append(outcome.error());
            } else {
                text.append("la salida no es la esperada.");
            }
        }
        return text.toString();
    }

    private RubricVersion current(UUID activityId) {
        return rubrics.findCurrent(activityId)
                .orElseThrow(() -> new IllegalStateException("Actividad publicada sin rúbrica: " + activityId));
    }

    @Override
    public void statementChanged(UUID activityId, String statement, UUID changedBy) {
        rubrics.findCurrent(activityId)
                .ifPresent(current -> rubrics.save(current.withStatement(statement, changedBy, clock.instant())));
    }
}
