package co.edu.unipamplona.logicode.activities.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.DeadlineException;
import co.edu.unipamplona.logicode.activities.domain.Submission;
import co.edu.unipamplona.logicode.identity.application.Actor;
import org.springframework.stereotype.Component;

/** Arma las vistas de actividades con los permisos del actor y, para un estudiante, su avance. */
@Component
class ActivityViews {

    private final ActivityAccess access;
    private final SubmissionRepository submissions;
    private final DeadlineExceptionRepository exceptions;
    private final Clock clock;

    ActivityViews(ActivityAccess access, SubmissionRepository submissions, DeadlineExceptionRepository exceptions,
            Clock clock) {
        this.access = access;
        this.submissions = submissions;
        this.exceptions = exceptions;
        this.clock = clock;
    }

    List<ActivityView> views(Actor actor, UUID courseId, List<Activity> activities) {
        if (activities.isEmpty()) {
            return List.of();
        }
        boolean canManage = access.canManage(actor, courseId);
        boolean canGrade = access.canGrade(actor, courseId);
        boolean canViewWork = access.canViewCourseWork(actor, courseId);
        boolean enrolled = access.isEnrolled(actor, courseId);
        Set<UUID> ids = activities.stream().map(Activity::getId).collect(Collectors.toSet());

        Map<UUID, List<Submission>> mine = Map.of();
        Map<UUID, DeadlineException> myExceptions = Map.of();
        if (enrolled) {
            mine = submissions.findByStudentAndActivities(actor.userId(), ids).stream()
                    .sorted(Comparator.comparingInt(Submission::getAttempt))
                    .collect(Collectors.groupingBy(Submission::getActivityId));
            myExceptions = exceptions.findByStudentAndActivities(actor.userId(), ids).stream()
                    .collect(Collectors.toMap(DeadlineException::getActivityId, Function.identity()));
        }
        Instant now = clock.instant();
        Map<UUID, List<Submission>> finalMine = mine;
        Map<UUID, DeadlineException> finalExceptions = myExceptions;
        return activities.stream().map(a -> {
            Integer submittedCount = canViewWork ? (int) submissions.findByActivity(a.getId()).stream()
                    .map(Submission::getStudentId).distinct().count() : null;
            ActivityView.StudentProgress progress = enrolled
                    ? progress(a, finalMine.getOrDefault(a.getId(), List.of()), finalExceptions.get(a.getId()), now)
                    : null;
            return new ActivityView(a.getId(), a.getCourseId(), a.getTitle(), a.getKind(), a.getStatement(),
                    a.isAcceptsText(), a.isAcceptsFiles(), a.isAcceptsPhotos(), a.getFileGroups(),
                    a.getWeightPercent(), a.getOpensAt(), a.getClosesAt(), a.getMaxAttempts(), a.getStatus(),
                    canManage, canGrade, canViewWork, submittedCount, progress);
        }).toList();
    }

    ActivityView view(Actor actor, Activity activity) {
        return views(actor, activity.getCourseId(), List.of(activity)).getFirst();
    }

    /** Reglas de RN-05: entrega abierta hasta el cierre o la ampliación, y dentro de los intentos permitidos. */
    static ActivityView.StudentProgress progress(Activity activity, List<Submission> attempts,
            DeadlineException exception, Instant now) {
        Instant deadline = activity.deadlineWith(exception);
        int allowed = activity.attemptsWith(exception);
        boolean open = activity.isOpenAt(now) && !now.isAfter(deadline);
        boolean canSubmit = activity.isPublished() && open && attempts.size() < allowed;
        ActivityView.State state;
        if (!attempts.isEmpty()) {
            state = ActivityView.State.SUBMITTED;
        } else if (!activity.isOpenAt(now)) {
            state = ActivityView.State.NOT_OPEN;
        } else if (now.isAfter(deadline)) {
            state = activity.isPractice() ? ActivityView.State.CLOSED : ActivityView.State.OVERDUE;
        } else {
            state = ActivityView.State.OPEN;
        }
        UUID last = attempts.isEmpty() ? null : attempts.getLast().getId();
        return new ActivityView.StudentProgress(state, attempts.size(), allowed, deadline, canSubmit, last,
                exception != null);
    }
}
