package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.GradeChange;
import co.edu.unipamplona.logicode.grading.domain.GradeSource;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calificación manual (Etapa 5): el docente del curso pone la nota de 0,0 a 5,0 y la retroalimentación, guiado por la
 * rúbrica. Publicar o corregir deja el cambio en el historial (RF-17, RN-09).
 */
@Service
public class GradeSubmissions {

    private final GradeRepository grades;
    private final GradeChangeRepository changes;
    private final RubricRepository rubrics;
    private final GradingAccess access;
    private final GradingViews views;
    private final Clock clock;

    public GradeSubmissions(GradeRepository grades, GradeChangeRepository changes, RubricRepository rubrics,
            GradingAccess access, GradingViews views, Clock clock) {
        this.grades = grades;
        this.changes = changes;
        this.rubrics = rubrics;
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

    @Transactional(readOnly = true)
    public GradeDetail get(Actor actor, UUID submissionId) {
        ActivityCatalog.SubmissionInfo submission = access.requireReadable(actor, submissionId);
        return detail(actor, submission, requireGrade(submissionId));
    }

    private GradeDetail detail(Actor actor, ActivityCatalog.SubmissionInfo submission, Grade grade) {
        boolean staff = access.canViewCourseWork(actor, submission.courseId());
        RubricVersion rubric = rubrics.findById(grade.getRubricVersionId()).orElseThrow();
        List<GradeDetail.Change> history = List.of();
        if (staff) {
            List<GradeChange> found = changes.findByGrade(grade.getId());
            Map<UUID, String> names = views.names(found.stream().map(GradeChange::getChangedBy).filter(Objects::nonNull)
                    .distinct().toList());
            history = found.stream().map(c -> new GradeDetail.Change(c.getPreviousScore(), c.getNewScore(),
                    c.getFeedback(), c.getSource(), names.get(c.getChangedBy()), c.getChangedAt())).toList();
        }
        return new GradeDetail(views.grade(grade, !staff), GradingViews.rubric(rubric, staff), history,
                access.canGrade(actor, submission.courseId()));
    }

    private Grade requireGrade(UUID submissionId) {
        return grades.findBySubmission(submissionId)
                .orElseThrow(() -> ApplicationException.notFound("La entrega no tiene registro de calificación."));
    }
}
