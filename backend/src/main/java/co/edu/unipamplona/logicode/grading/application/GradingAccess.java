package co.edu.unipamplona.logicode.grading.application;

import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Component;

/** Quién ve y quién pone las notas: el autor ve las suyas; el curso, quien ve sus entregas; califica su docente. */
@Component
class GradingAccess {

    private final ActivityCatalog catalog;
    private final CourseAccess courseAccess;

    GradingAccess(ActivityCatalog catalog, CourseAccess courseAccess) {
        this.catalog = catalog;
        this.courseAccess = courseAccess;
    }

    /** Entrega legible por el actor: la suya o una del curso cuyas entregas ve. Para los demás no existe. */
    ActivityCatalog.SubmissionInfo requireReadable(Actor actor, UUID submissionId) {
        ActivityCatalog.SubmissionInfo submission = catalog.submission(submissionId)
                .orElseThrow(() -> ApplicationException.notFound("La entrega no existe."));
        if (!isOwner(actor, submission) && !canViewCourseWork(actor, submission.courseId())) {
            throw ApplicationException.notFound("La entrega no existe.");
        }
        return submission;
    }

    ActivityCatalog.SubmissionInfo requireGradable(Actor actor, UUID submissionId) {
        ActivityCatalog.SubmissionInfo submission = requireReadable(actor, submissionId);
        if (!canGrade(actor, submission.courseId())) {
            throw ApplicationException.forbidden("Solo el docente del curso puede calificar.");
        }
        return submission;
    }

    void requireViewCourseWork(Actor actor, UUID courseId) {
        courseAccess.requireViewable(actor, courseId);
        if (!canViewCourseWork(actor, courseId)) {
            throw ApplicationException.forbidden("Solo el docente del curso o un Administrador ve las notas del curso.");
        }
    }

    boolean isOwner(Actor actor, ActivityCatalog.SubmissionInfo submission) {
        return actor != null && submission.studentId().equals(actor.userId());
    }

    boolean canViewCourseWork(Actor actor, UUID courseId) {
        return courseAccess.canViewCourseWork(actor, courseId);
    }

    boolean canGrade(Actor actor, UUID courseId) {
        return courseAccess.canGrade(actor, courseId);
    }
}
