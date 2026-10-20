package co.edu.unipamplona.logicode.activities.application;

import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Component;

/**
 * Quién ve, administra o entrega una actividad. Un borrador solo lo ve quien administra el curso; lo que el actor no
 * puede ver responde como inexistente.
 */
@Component
class ActivityAccess {

    private final ActivityRepository activities;
    private final CourseAccess courseAccess;

    ActivityAccess(ActivityRepository activities, CourseAccess courseAccess) {
        this.activities = activities;
        this.courseAccess = courseAccess;
    }

    Activity requireVisible(Actor actor, UUID activityId) {
        Activity activity = activities.findById(activityId).filter(a -> !a.isArchived())
                .orElseThrow(() -> ApplicationException.notFound("La actividad no existe."));
        courseAccess.requireViewable(actor, activity.getCourseId());
        if (!activity.isPublished() && !courseAccess.canManageContent(actor, activity.getCourseId())) {
            throw ApplicationException.notFound("La actividad no existe.");
        }
        return activity;
    }

    Activity requireManageable(Actor actor, UUID activityId) {
        Activity activity = requireVisible(actor, activityId);
        courseAccess.requireManageContent(actor, activity.getCourseId());
        return activity;
    }

    void requireViewCourseWork(Actor actor, UUID courseId) {
        if (!courseAccess.canViewCourseWork(actor, courseId)) {
            throw ApplicationException.forbidden("Solo el docente del curso o un Administrador ve las entregas.");
        }
    }

    boolean canManage(Actor actor, UUID courseId) {
        return courseAccess.canManageContent(actor, courseId);
    }

    boolean canGrade(Actor actor, UUID courseId) {
        return courseAccess.canGrade(actor, courseId);
    }

    boolean canViewCourseWork(Actor actor, UUID courseId) {
        return courseAccess.canViewCourseWork(actor, courseId);
    }

    boolean isEnrolled(Actor actor, UUID courseId) {
        return courseAccess.isEnrolled(actor, courseId);
    }
}
