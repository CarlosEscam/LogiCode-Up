package co.edu.unipamplona.logicode.activities.application;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.DeadlineException;
import co.edu.unipamplona.logicode.activities.domain.FileGroup;
import co.edu.unipamplona.logicode.activities.domain.Submission;
import co.edu.unipamplona.logicode.files.application.AcceptedFiles;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-13 y RN-05: el estudiante inscrito entrega texto, archivos o fotos mientras la actividad está abierta y le
 * quedan intentos. Después del cierre solo entrega si el docente le amplió el plazo.
 */
@Service
public class SubmitWork {

    /** Archivo recibido. */
    public record Upload(InputStream content, String originalName) {
    }

    private final ActivityAccess access;
    private final SubmissionRepository submissions;
    private final DeadlineExceptionRepository exceptions;
    private final FileStore fileStore;
    private final GradingGateway grading;
    private final SubmissionViews views;
    private final Clock clock;

    public SubmitWork(ActivityAccess access, SubmissionRepository submissions, DeadlineExceptionRepository exceptions,
            FileStore fileStore, GradingGateway grading, SubmissionViews views, Clock clock) {
        this.access = access;
        this.submissions = submissions;
        this.exceptions = exceptions;
        this.fileStore = fileStore;
        this.grading = grading;
        this.views = views;
        this.clock = clock;
    }

    @Transactional
    public SubmissionView submit(Actor actor, UUID activityId, String text, List<Upload> uploads) {
        actor.require(Permission.ANSWER_ACTIVITIES);
        Activity activity = access.requireVisible(actor, activityId);
        if (!access.isEnrolled(actor, activity.getCourseId())) {
            throw ApplicationException.forbidden("Solo los estudiantes inscritos en el curso pueden entregar.");
        }
        Instant now = clock.instant();
        DeadlineException exception = exceptions.find(activityId, actor.userId()).orElse(null);
        List<Submission> previous = submissions.findByActivityAndStudent(activityId, actor.userId());
        ActivityView.StudentProgress progress = ActivityViews.progress(activity, previous, exception, now);
        if (!activity.isOpenAt(now)) {
            throw ApplicationException.businessRule("La actividad aún no está abierta.");
        }
        if (now.isAfter(progress.deadline())) {
            throw ApplicationException.businessRule("La actividad ya cerró. Solo el docente puede ampliar tu plazo.");
        }
        if (progress.attemptsUsed() >= progress.attemptsAllowed()) {
            throw ApplicationException.businessRule("Ya usaste los intentos permitidos para esta actividad.");
        }

        String answer = clean(text);
        if (answer != null && !activity.isAcceptsText()) {
            throw ApplicationException.businessRule("Esta actividad no recibe respuestas en texto.");
        }
        List<Upload> files = uploads == null ? List.of() : uploads;
        if (!files.isEmpty() && !(activity.isAcceptsFiles() || activity.isAcceptsPhotos())) {
            throw ApplicationException.businessRule("Esta actividad no recibe archivos.");
        }
        if (files.size() > Submission.MAX_FILES) {
            throw ApplicationException.businessRule("Puedes adjuntar hasta " + Submission.MAX_FILES + " archivos.");
        }
        if (answer == null && files.isEmpty()) {
            throw ApplicationException.businessRule("La entrega está vacía: escribe tu respuesta o adjunta un archivo.");
        }

        Set<AcceptedFiles> accepted = accepted(activity);
        List<UUID> fileIds = new ArrayList<>();
        for (Upload upload : files) {
            fileIds.add(fileStore.store(upload.content(), upload.originalName(), actor.userId(), accepted).id());
        }
        Submission saved = submissions.save(new Submission(activityId, actor.userId(), previous.size() + 1, answer,
                fileIds, activity.isClosedAt(now), now));
        grading.submissionReceived(saved.getId(), activityId);
        return views.view(saved, activity);
    }

    /** Las fotos son imágenes; los archivos, los grupos que eligió el docente. */
    private static Set<AcceptedFiles> accepted(Activity activity) {
        Set<AcceptedFiles> accepted = EnumSet.noneOf(AcceptedFiles.class);
        if (activity.isAcceptsPhotos()) {
            accepted.add(AcceptedFiles.IMAGES);
        }
        if (activity.isAcceptsFiles()) {
            for (FileGroup group : activity.getFileGroups()) {
                accepted.add(AcceptedFiles.valueOf(group.name()));
            }
        }
        return accepted;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
