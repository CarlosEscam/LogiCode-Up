package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.GradeStatus;
import co.edu.unipamplona.logicode.grading.domain.ReviewReason;
import co.edu.unipamplona.logicode.identity.application.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bandeja Por revisar y consulta de notas (RF-17, RF-19). Cuenta el último intento de cada estudiante: los intentos
 * anteriores conservan su nota en el historial pero no vuelven a la bandeja.
 */
@Service
public class BrowseGrades {

    /** Entrega esperando calificación: en evaluación con IA o esperando al docente, con el motivo. */
    public record ReviewItem(UUID submissionId, UUID activityId, String activityTitle, UUID studentId,
            String studentName, int attempt, Instant submittedAt, boolean late, int rubricVersion,
            GradeStatus status, ReviewReason reviewReason) {
    }

    /** Fila de notas de una actividad para el docente: un intento con su nota. */
    public record GradeRow(UUID submissionId, UUID studentId, String studentName, int attempt, Instant submittedAt,
            boolean late, boolean latestAttempt, GradeView grade) {
    }

    /** Nota del estudiante en una actividad (su último intento). */
    public record MyGrade(UUID activityId, UUID submissionId, int attempt, GradeStatus status, BigDecimal score,
            String feedback, Instant gradedAt) {
    }

    private final GradeRepository grades;
    private final ActivityCatalog catalog;
    private final CourseAccess courseAccess;
    private final GradingAccess access;
    private final GradingViews views;

    public BrowseGrades(GradeRepository grades, ActivityCatalog catalog, CourseAccess courseAccess,
            GradingAccess access, GradingViews views) {
        this.grades = grades;
        this.catalog = catalog;
        this.courseAccess = courseAccess;
        this.access = access;
        this.views = views;
    }

    /** Últimos intentos sin nota publicada del curso, del más antiguo al más reciente. */
    @Transactional(readOnly = true)
    public List<ReviewItem> reviewQueue(Actor actor, UUID courseId) {
        access.requireViewCourseWork(actor, courseId);
        List<ActivityCatalog.SubmissionInfo> latest = catalog.activitiesOf(courseId).stream()
                .flatMap(a -> latestAttempts(catalog.submissions(catalog.submissionIdsOf(a.id())).values()).stream())
                .toList();
        Map<UUID, Grade> pending = grades.findBySubmissions(latest.stream().map(ActivityCatalog.SubmissionInfo::id)
                .toList()).stream().filter(g -> !g.isPublished())
                .collect(Collectors.toMap(Grade::getSubmissionId, Function.identity()));
        Map<UUID, GradeView> pendingViews = views.grades(pending.values(), false);
        return latest.stream().filter(s -> pending.containsKey(s.id()))
                .sorted(Comparator.comparing(ActivityCatalog.SubmissionInfo::submittedAt))
                .map(s -> {
                    GradeView g = pendingViews.get(s.id());
                    return new ReviewItem(s.id(), s.activityId(), s.activityTitle(), s.studentId(), s.studentName(),
                            s.attempt(), s.submittedAt(), s.late(), g.rubricVersion(), g.status(), g.reviewReason());
                })
                .toList();
    }

    /** Todos los intentos de la actividad con su nota. */
    @Transactional(readOnly = true)
    public List<GradeRow> forActivity(Actor actor, UUID activityId) {
        ActivityCatalog.ActivityInfo activity = catalog.requireVisible(actor, activityId);
        access.requireViewCourseWork(actor, activity.courseId());
        Collection<ActivityCatalog.SubmissionInfo> all = catalog.submissions(catalog.submissionIdsOf(activityId))
                .values();
        Set<UUID> latest = latestAttempts(all).stream().map(ActivityCatalog.SubmissionInfo::id)
                .collect(Collectors.toSet());
        Map<UUID, GradeView> byId = views.grades(grades.findBySubmissions(all.stream()
                .map(ActivityCatalog.SubmissionInfo::id).toList()), false);
        return all.stream()
                .sorted(Comparator.comparing(ActivityCatalog.SubmissionInfo::studentName,
                        Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(ActivityCatalog.SubmissionInfo::attempt))
                .map(s -> new GradeRow(s.id(), s.studentId(), s.studentName(), s.attempt(), s.submittedAt(), s.late(),
                        latest.contains(s.id()), byId.get(s.id())))
                .toList();
    }

    /** Notas del propio estudiante en el curso; las no publicadas aparecen en revisión, sin nota. */
    @Transactional(readOnly = true)
    public List<MyGrade> mine(Actor actor, UUID courseId) {
        courseAccess.requireViewable(actor, courseId);
        Collection<ActivityCatalog.SubmissionInfo> latest = latestAttempts(
                catalog.submissions(catalog.submissionIdsOf(actor.userId(), courseId)).values());
        Map<UUID, GradeView> byId = views.grades(grades.findBySubmissions(latest.stream()
                .map(ActivityCatalog.SubmissionInfo::id).toList()), true);
        return latest.stream().filter(s -> byId.containsKey(s.id())).map(s -> {
            GradeView g = byId.get(s.id());
            return new MyGrade(s.activityId(), s.id(), s.attempt(), g.status(), g.score(), g.feedback(),
                    g.gradedAt());
        }).toList();
    }

    private static List<ActivityCatalog.SubmissionInfo> latestAttempts(
            Collection<ActivityCatalog.SubmissionInfo> submissions) {
        return List.copyOf(submissions.stream().collect(Collectors.toMap(
                s -> s.activityId() + ":" + s.studentId(), Function.identity(),
                (a, b) -> a.attempt() >= b.attempt() ? a : b)).values());
    }
}
