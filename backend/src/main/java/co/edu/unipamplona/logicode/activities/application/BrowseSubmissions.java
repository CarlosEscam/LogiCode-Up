package co.edu.unipamplona.logicode.activities.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.DeadlineException;
import co.edu.unipamplona.logicode.activities.domain.Submission;
import co.edu.unipamplona.logicode.courses.application.CourseRoster;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-17 y RF-19: el estudiante consulta sus entregas; el docente del curso y las cuentas Administrador consultan las
 * de todos, incluido quién no ha entregado.
 */
@Service
public class BrowseSubmissions {

    /**
     * Situación de un inscrito en la actividad. {@code studentId} es nulo si aún no se registra; {@code progress}
     * aplica las mismas reglas de plazo e intentos que ve el estudiante.
     */
    public record StudentWork(String cedula, String fullName, UUID studentId, int attempts, UUID lastSubmissionId,
            Instant lastSubmittedAt, boolean lastLate, ExceptionInfo exception,
            ActivityView.StudentProgress progress) {
    }

    public record ExceptionInfo(Instant extendedUntil, int extraAttempts, String reason) {
    }

    private final ActivityAccess access;
    private final SubmissionRepository submissions;
    private final DeadlineExceptionRepository exceptions;
    private final CourseRoster roster;
    private final SubmissionViews views;
    private final FileStore fileStore;
    private final ActivityRepository activities;
    private final Clock clock;

    public BrowseSubmissions(ActivityAccess access, SubmissionRepository submissions,
            DeadlineExceptionRepository exceptions, CourseRoster roster, SubmissionViews views, FileStore fileStore,
            ActivityRepository activities, Clock clock) {
        this.access = access;
        this.submissions = submissions;
        this.exceptions = exceptions;
        this.roster = roster;
        this.views = views;
        this.fileStore = fileStore;
        this.activities = activities;
        this.clock = clock;
    }

    /** Intentos del propio estudiante, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<SubmissionView> mine(Actor actor, UUID activityId) {
        Activity activity = access.requireVisible(actor, activityId);
        return submissions.findByActivityAndStudent(activityId, actor.userId()).stream()
                .sorted(Comparator.comparingInt(Submission::getAttempt).reversed())
                .map(s -> views.view(s, activity)).toList();
    }

    /** Todos los inscritos con su situación en la actividad. */
    @Transactional(readOnly = true)
    public List<StudentWork> forActivity(Actor actor, UUID activityId) {
        Activity activity = access.requireVisible(actor, activityId);
        access.requireViewCourseWork(actor, activity.getCourseId());
        return work(activity, roster.members(activity.getCourseId()));
    }

    /** Situación de cada inscrito en la actividad, sin verificar permisos (también la usa la planilla). */
    List<StudentWork> work(Activity activity, List<CourseRoster.Member> members) {
        UUID activityId = activity.getId();
        Map<UUID, List<Submission>> byStudent = submissions.findByActivity(activityId).stream()
                .sorted(Comparator.comparingInt(Submission::getAttempt))
                .collect(Collectors.groupingBy(Submission::getStudentId));
        Map<UUID, DeadlineException> byException = exceptions.findByActivity(activityId).stream()
                .collect(Collectors.toMap(DeadlineException::getStudentId, Function.identity()));
        Instant now = clock.instant();
        return members.stream().map(member -> {
            List<Submission> attempts = member.userId() == null ? List.of()
                    : byStudent.getOrDefault(member.userId(), List.of());
            DeadlineException exception = member.userId() == null ? null : byException.get(member.userId());
            Submission last = attempts.isEmpty() ? null : attempts.getLast();
            return new StudentWork(member.cedula(), member.fullName(), member.userId(), attempts.size(),
                    last == null ? null : last.getId(), last == null ? null : last.getSubmittedAt(),
                    last != null && last.isLate(),
                    exception == null ? null : new ExceptionInfo(exception.getExtendedUntil(),
                            exception.getExtraAttempts(), exception.getReason()),
                    ActivityViews.progress(activity, attempts, exception, now));
        }).toList();
    }

    @Transactional(readOnly = true)
    public SubmissionView get(Actor actor, UUID submissionId) {
        Submission submission = requireReadable(actor, submissionId);
        return views.view(submission, activities.findById(submission.getActivityId()).orElseThrow());
    }

    /** Archivo de una entrega. Con {@code preview} solo formatos que el navegador muestra (PDF, imágenes, videos). */
    @Transactional(readOnly = true)
    public FileStore.FileContent openFile(Actor actor, UUID submissionId, UUID fileId, boolean preview) {
        Submission submission = requireReadable(actor, submissionId);
        if (!submission.getFileIds().contains(fileId)) {
            throw ApplicationException.notFound("El archivo no existe.");
        }
        FileStore.FileContent content = fileStore.open(fileId);
        if (preview && !content.info().previewable()) {
            throw ApplicationException.businessRule("Este formato no tiene vista previa; descárgalo para abrirlo.");
        }
        return content;
    }

    /** La entrega la ve su autor y quien consulta las entregas del curso; para los demás no existe. */
    private Submission requireReadable(Actor actor, UUID submissionId) {
        Submission submission = submissions.findById(submissionId)
                .orElseThrow(() -> ApplicationException.notFound("La entrega no existe."));
        Activity activity = access.requireVisible(actor, submission.getActivityId());
        boolean own = actor != null && submission.getStudentId().equals(actor.userId());
        if (!own && !access.canViewCourseWork(actor, activity.getCourseId())) {
            throw ApplicationException.notFound("La entrega no existe.");
        }
        return submission;
    }
}
