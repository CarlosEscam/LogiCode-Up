package co.edu.unipamplona.logicode.activities.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.DeadlineException;
import co.edu.unipamplona.logicode.courses.application.CourseRoster;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RN-05: el docente del curso amplía el plazo de un estudiante (también después del cierre, para reabrir) o le da
 * intentos adicionales. Una excepción por estudiante y actividad; guardarla de nuevo la reemplaza.
 */
@Service
public class ManageDeadlineExceptions {

    private final ActivityAccess access;
    private final DeadlineExceptionRepository exceptions;
    private final CourseRoster roster;
    private final Clock clock;

    public ManageDeadlineExceptions(ActivityAccess access, DeadlineExceptionRepository exceptions,
            CourseRoster roster, Clock clock) {
        this.access = access;
        this.exceptions = exceptions;
        this.roster = roster;
        this.clock = clock;
    }

    @Transactional
    public void grant(Actor actor, UUID activityId, UUID studentId, Instant extendedUntil, int extraAttempts,
            String reason) {
        Activity activity = access.requireManageable(actor, activityId);
        boolean member = roster.members(activity.getCourseId()).stream()
                .anyMatch(m -> studentId.equals(m.userId()));
        if (!member) {
            throw ApplicationException.notFound("El estudiante no está inscrito en el curso o aún no se registra.");
        }
        Instant now = clock.instant();
        if (extendedUntil == null && extraAttempts <= 0) {
            throw ApplicationException.businessRule("Amplía el plazo o da por lo menos un intento adicional.");
        }
        if (extraAttempts < 0 || extraAttempts > Activity.MAX_ATTEMPTS) {
            throw ApplicationException.businessRule("Los intentos adicionales deben estar entre 0 y "
                    + Activity.MAX_ATTEMPTS + ".");
        }
        if (extendedUntil != null && !extendedUntil.isAfter(now)) {
            throw ApplicationException.businessRule("El nuevo plazo debe ser una fecha futura.");
        }
        DeadlineException exception = exceptions.find(activityId, studentId)
                .orElseGet(() -> new DeadlineException(activityId, studentId));
        String cleanReason = reason == null || reason.isBlank() ? null : reason.strip();
        exception.grant(extendedUntil, extraAttempts, cleanReason, actor.userId(), now);
        exceptions.save(exception);
    }

    @Transactional
    public void revoke(Actor actor, UUID activityId, UUID studentId) {
        access.requireManageable(actor, activityId);
        DeadlineException exception = exceptions.find(activityId, studentId)
                .orElseThrow(() -> ApplicationException.notFound("Este estudiante no tiene una excepción de plazo."));
        exceptions.delete(exception);
    }
}
