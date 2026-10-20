package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.grading.domain.AiGradingJob;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.GradeChange;
import co.edu.unipamplona.logicode.grading.domain.GradeSource;
import co.edu.unipamplona.logicode.grading.domain.GradeStatus;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calificación por el docente del curso: pone o confirma la nota de 0,0 a 5,0 y la retroalimentación, guiado por la
 * rúbrica y, si existe, por la propuesta de la IA. Publicar o corregir deja el cambio en el historial (RF-17, RN-09).
 * También puede devolver a la IA una entrega que espera en Por revisar (RF-15: reintento tras un fallo técnico).
 */
@Service
public class GradeSubmissions {

    private final GradeRepository grades;
    private final GradeChangeRepository changes;
    private final RubricRepository rubrics;
    private final AiEvaluationRepository evaluations;
    private final AiGradingJobRepository jobs;
    private final AiSubmissionContent content;
    private final AiGradingSettings aiSettings;
    private final GradingAccess access;
    private final GradingViews views;
    private final Clock clock;

    public GradeSubmissions(GradeRepository grades, GradeChangeRepository changes, RubricRepository rubrics,
            AiEvaluationRepository evaluations, AiGradingJobRepository jobs, AiSubmissionContent content,
            AiGradingSettings aiSettings, GradingAccess access, GradingViews views, Clock clock) {
        this.grades = grades;
        this.changes = changes;
        this.rubrics = rubrics;
        this.evaluations = evaluations;
        this.jobs = jobs;
        this.content = content;
        this.aiSettings = aiSettings;
        this.access = access;
        this.views = views;
        this.clock = clock;
    }

    @Transactional
    public GradeDetail grade(Actor actor, UUID submissionId, BigDecimal score, String feedback) {
        ActivityCatalog.SubmissionInfo submission = access.requireGradable(actor, submissionId);
        if (!Grade.isValidScore(score)) {
            throw ApplicationException.businessRule("La nota debe estar entre 0,0 y 5,0, con un decimal.");
        }
        Grade grade = requireGrade(submissionId);
        String cleanFeedback = feedback == null || feedback.isBlank() ? null : feedback.strip();
        changes.save(grade.publish(score, cleanFeedback, GradeSource.TEACHER, actor.userId(), clock.instant()));
        return detail(actor, submission, grades.save(grade));
    }

    /** Devuelve a la cola de IA una entrega en Por revisar. Una nota publicada solo la corrige el docente. */
    @Transactional
    public GradeDetail retryWithAi(Actor actor, UUID submissionId) {
        ActivityCatalog.SubmissionInfo submission = access.requireGradable(actor, submissionId);
        if (!aiSettings.enabled()) {
            throw ApplicationException.businessRule("La calificación con IA está desactivada.");
        }
        Grade grade = requireGrade(submissionId);
        if (grade.getStatus() != GradeStatus.PENDING_REVIEW) {
            throw ApplicationException.conflict(grade.isPublished()
                    ? "La nota ya está publicada; el docente puede corregirla."
                    : "La entrega ya se está evaluando con IA.");
        }
        if (!content.isReadable(submissionId)) {
            throw ApplicationException.businessRule(
                    "La IA solo lee respuestas escritas, fotos PNG o JPG y archivos de PSeInt o Arduino.");
        }
        Instant now = clock.instant();
        grade.startAiEvaluation(now);
        grade = grades.save(grade);
        jobs.save(new AiGradingJob(grade.getId(), actor.userId(), now));
        return detail(actor, submission, grade);
    }

    @Transactional(readOnly = true)
    public GradeDetail get(Actor actor, UUID submissionId) {
        ActivityCatalog.SubmissionInfo submission = access.requireReadable(actor, submissionId);
        return detail(actor, submission, requireGrade(submissionId));
    }

    private GradeDetail detail(Actor actor, ActivityCatalog.SubmissionInfo submission, Grade grade) {
        boolean staff = access.canViewCourseWork(actor, submission.courseId());
        RubricVersion rubric = rubrics.findById(grade.getRubricVersionId()).orElseThrow();
        List<GradeDetail.Change> history = List.of();
        List<AiEvaluationView> aiEvaluations = List.of();
        if (staff) {
            aiEvaluations = evaluations.findByGrade(grade.getId()).stream().map(AiEvaluationView::of).toList();
            List<GradeChange> found = changes.findByGrade(grade.getId());
            Map<UUID, String> names = views.names(found.stream().map(GradeChange::getChangedBy).filter(Objects::nonNull)
                    .distinct().toList());
            history = found.stream().map(c -> new GradeDetail.Change(c.getPreviousScore(), c.getNewScore(),
                    c.getFeedback(), c.getSource(), names.get(c.getChangedBy()), c.getChangedAt())).toList();
        }
        boolean canGrade = access.canGrade(actor, submission.courseId());
        boolean canRetry = canGrade && aiSettings.enabled() && grade.getStatus() == GradeStatus.PENDING_REVIEW
                && content.isReadable(submission.id());
        return new GradeDetail(views.grade(grade, !staff), GradingViews.rubric(rubric, staff), history,
                aiEvaluations, canGrade, canRetry);
    }

    private Grade requireGrade(UUID submissionId) {
        return grades.findBySubmission(submissionId)
                .orElseThrow(() -> ApplicationException.notFound("La entrega no tiene registro de calificación."));
    }
}
