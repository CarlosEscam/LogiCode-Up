package co.edu.unipamplona.logicode.activities.application;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.Submission;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas para el módulo grading: actividades y entregas, y la verificación de quién puede verlas. No expone las
 * entidades del dominio.
 */
@Service
public class ActivityCatalog {

    public record ActivityInfo(UUID id, UUID courseId, String title, String statement, Instant closesAt,
            boolean published) {
    }

    public record SubmissionInfo(UUID id, UUID activityId, String activityTitle, UUID courseId, UUID studentId,
            String studentName, int attempt, Instant submittedAt, boolean late) {
    }

    /** Lo entregado: respuesta escrita y archivos en el orden en que se cargaron. */
    public record SubmissionContent(String textAnswer, List<UUID> fileIds) {
    }

    private final ActivityRepository activities;
    private final SubmissionRepository submissions;
    private final ActivityAccess access;
    private final UserDirectory directory;

    public ActivityCatalog(ActivityRepository activities, SubmissionRepository submissions, ActivityAccess access,
            UserDirectory directory) {
        this.activities = activities;
        this.submissions = submissions;
        this.access = access;
        this.directory = directory;
    }

    @Transactional(readOnly = true)
    public Optional<ActivityInfo> activity(UUID activityId) {
        return activities.findById(activityId).filter(a -> !a.isArchived()).map(ActivityCatalog::info);
    }

    /** La actividad, si el actor puede administrarla (404/403 si no). */
    @Transactional(readOnly = true)
    public ActivityInfo requireManageable(Actor actor, UUID activityId) {
        return info(access.requireManageable(actor, activityId));
    }

    /** La actividad, si el actor puede verla (404 si no). */
    @Transactional(readOnly = true)
    public ActivityInfo requireVisible(Actor actor, UUID activityId) {
        return info(access.requireVisible(actor, activityId));
    }

    @Transactional(readOnly = true)
    public List<ActivityInfo> activitiesOf(UUID courseId) {
        return activities.findActiveByCourse(courseId).stream().map(ActivityCatalog::info).toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, SubmissionInfo> submissions(Collection<UUID> submissionIds) {
        List<Submission> found = submissions.findAllById(submissionIds);
        Map<UUID, Activity> byActivity = found.stream().map(Submission::getActivityId).distinct()
                .map(activities::findById).flatMap(Optional::stream)
                .collect(Collectors.toMap(Activity::getId, Function.identity()));
        Map<UUID, String> names = directory.fullNamesOf(found.stream().map(Submission::getStudentId).toList());
        return found.stream().filter(s -> byActivity.containsKey(s.getActivityId())).map(s -> {
            Activity a = byActivity.get(s.getActivityId());
            return new SubmissionInfo(s.getId(), a.getId(), a.getTitle(), a.getCourseId(), s.getStudentId(),
                    names.get(s.getStudentId()), s.getAttempt(), s.getSubmittedAt(), s.isLate());
        }).collect(Collectors.toMap(SubmissionInfo::id, Function.identity()));
    }

    @Transactional(readOnly = true)
    public Optional<SubmissionInfo> submission(UUID submissionId) {
        return Optional.ofNullable(submissions(List.of(submissionId)).get(submissionId));
    }

    /** Contenido de la entrega, para evaluarla con IA. */
    @Transactional(readOnly = true)
    public Optional<SubmissionContent> content(UUID submissionId) {
        return submissions.findById(submissionId).map(s -> new SubmissionContent(s.getTextAnswer(), s.getFileIds()));
    }

    /** Entregas de una actividad, para consultar sus notas. */
    @Transactional(readOnly = true)
    public List<UUID> submissionIdsOf(UUID activityId) {
        return submissions.findByActivity(activityId).stream().map(Submission::getId).toList();
    }

    /** Entregas del estudiante en las actividades del curso. */
    @Transactional(readOnly = true)
    public List<UUID> submissionIdsOf(UUID studentId, UUID courseId) {
        List<UUID> ids = activities.findActiveByCourse(courseId).stream().map(Activity::getId).toList();
        return ids.isEmpty() ? List.of()
                : submissions.findByStudentAndActivities(studentId, ids).stream().map(Submission::getId).toList();
    }

    private static ActivityInfo info(Activity a) {
        return new ActivityInfo(a.getId(), a.getCourseId(), a.getTitle(), a.getStatement(), a.getClosesAt(),
                a.isPublished());
    }
}
