package co.edu.unipamplona.logicode.activities.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.ActivityKind;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-10 y RF-11: el docente del curso crea actividades como borrador, define su rúbrica (módulo grading) y las
 * publica. Los estudiantes inscritos ven las publicadas.
 */
@Service
public class ManageActivities {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ActivityRepository activities;
    private final ActivityAccess access;
    private final ActivityViews views;
    private final CourseAccess courseAccess;
    private final GradingGateway grading;
    private final Clock clock;

    public ManageActivities(ActivityRepository activities, ActivityAccess access, ActivityViews views,
            CourseAccess courseAccess, GradingGateway grading, Clock clock) {
        this.activities = activities;
        this.access = access;
        this.views = views;
        this.courseAccess = courseAccess;
        this.grading = grading;
        this.clock = clock;
    }

    @Transactional
    public ActivityView create(Actor actor, UUID courseId, Activity.Definition definition) {
        courseAccess.requireManageContent(actor, courseId);
        Activity.Definition valid = validate(definition);
        requireWeightFits(courseId, null, valid.weightPercent());
        Activity activity = activities.save(new Activity(courseId, valid, actor.userId(), clock.instant()));
        return views.view(actor, activity);
    }

    @Transactional
    public ActivityView update(Actor actor, UUID activityId, Activity.Definition definition) {
        Activity activity = access.requireManageable(actor, activityId);
        Activity.Definition valid = validate(definition);
        if (activity.isQuiz() != valid.kind().isQuiz()) {
            throw ApplicationException.businessRule("Una actividad no se puede convertir en quiz ni un quiz en "
                    + "actividad; crea una nueva.");
        }
        requireWeightFits(activity.getCourseId(), activity.getId(), valid.weightPercent());
        boolean statementChanged = !Objects.equals(activity.getStatement(), valid.statement());
        activity.update(valid, clock.instant());
        activities.save(activity);
        if (statementChanged) {
            grading.statementChanged(activity.getId(), valid.statement(), actor.userId());
        }
        return views.view(actor, activity);
    }

    /** RN-20: se publica con la rúbrica definida; la de un quiz se arma al guardar sus preguntas. */
    @Transactional
    public ActivityView publish(Actor actor, UUID activityId) {
        Activity activity = access.requireManageable(actor, activityId);
        if (!grading.hasRubric(activityId)) {
            throw ApplicationException.businessRule(activity.isQuiz()
                    ? "Elige las preguntas del quiz antes de publicarlo."
                    : "Define la rúbrica de la actividad antes de publicarla.");
        }
        activity.publish(clock.instant());
        return views.view(actor, activities.save(activity));
    }

    /** Archiva la actividad: deja de mostrarse; sus entregas y archivos se conservan. */
    @Transactional
    public void archive(Actor actor, UUID activityId) {
        Activity activity = access.requireManageable(actor, activityId);
        activity.archive(clock.instant());
        activities.save(activity);
    }

    @Transactional(readOnly = true)
    public ActivityView get(Actor actor, UUID activityId) {
        return views.view(actor, access.requireVisible(actor, activityId));
    }

    /** Actividades del curso por fecha de cierre; los borradores solo para quien administra el curso. */
    @Transactional(readOnly = true)
    public List<ActivityView> list(Actor actor, UUID courseId) {
        courseAccess.requireViewable(actor, courseId);
        boolean canManage = access.canManage(actor, courseId);
        List<Activity> visible = activities.findActiveByCourse(courseId).stream()
                .filter(a -> canManage || a.isPublished()).toList();
        return views.views(actor, courseId, visible);
    }

    private static Activity.Definition validate(Activity.Definition d) {
        String title = clean(d.title());
        String statement = clean(d.statement());
        if (title == null) {
            throw ApplicationException.businessRule("El título de la actividad es obligatorio.");
        }
        if (statement == null) {
            throw ApplicationException.businessRule("Escribe el enunciado de la actividad.");
        }
        if (d.kind() == null) {
            throw ApplicationException.businessRule("Elige el tipo de actividad.");
        }
        if (d.kind().isQuiz()) {
            return validateQuiz(title, statement, d);
        }
        if (!(d.acceptsText() || d.acceptsFiles() || d.acceptsPhotos())) {
            throw ApplicationException.businessRule("Elige por lo menos una forma de entrega: texto, archivo o foto.");
        }
        if (d.acceptsFiles() && (d.fileGroups() == null || d.fileGroups().isEmpty())) {
            throw ApplicationException.businessRule("Elige qué tipos de archivo se aceptan.");
        }
        validateSchedule(d);
        return new Activity.Definition(title, d.kind(), statement, d.acceptsText(), d.acceptsFiles(),
                d.acceptsPhotos(), d.fileGroups(), d.weightPercent(), d.opensAt(), d.closesAt(), d.maxAttempts());
    }

    /** Un quiz no recibe texto ni archivos: se responde en su página. El de repaso no tiene porcentaje (RF-21). */
    private static Activity.Definition validateQuiz(String title, String statement, Activity.Definition d) {
        validateSchedule(d);
        boolean practice = d.kind() == ActivityKind.PRACTICE_QUIZ;
        if (practice && d.weightPercent() != null) {
            throw ApplicationException.businessRule("Un quiz de repaso no tiene nota, así que no lleva porcentaje.");
        }
        return new Activity.Definition(title, d.kind(), statement, false, false, false, Set.of(), d.weightPercent(),
                d.opensAt(), d.closesAt(), practice ? 1 : d.maxAttempts());
    }

    private static void validateSchedule(Activity.Definition d) {
        if (d.opensAt() == null || d.closesAt() == null) {
            throw ApplicationException.businessRule("Indica la apertura y el cierre.");
        }
        if (!d.closesAt().isAfter(d.opensAt())) {
            throw ApplicationException.businessRule("El cierre debe ser posterior a la apertura.");
        }
        if (d.maxAttempts() < 1 || d.maxAttempts() > Activity.MAX_ATTEMPTS) {
            throw ApplicationException.businessRule("Los intentos deben estar entre 1 y " + Activity.MAX_ATTEMPTS + ".");
        }
        BigDecimal weight = d.weightPercent();
        if (weight != null && (weight.signum() <= 0 || weight.compareTo(HUNDRED) > 0 || weight.scale() > 2)) {
            throw ApplicationException.businessRule("El porcentaje debe estar entre 0,01 y 100.");
        }
    }

    /** RN-07: la suma de los porcentajes explícitos del curso no puede superar 100 %. */
    private void requireWeightFits(UUID courseId, UUID excludedId, BigDecimal weight) {
        if (weight == null) {
            return;
        }
        BigDecimal others = activities.findActiveByCourse(courseId).stream()
                .filter(a -> !a.getId().equals(excludedId))
                .map(Activity::getWeightPercent).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (others.add(weight).compareTo(HUNDRED) > 0) {
            throw ApplicationException.businessRule("Los porcentajes del curso sumarían " + percent(others.add(weight))
                    + " %. La suma no puede superar 100 % (disponible: " + percent(HUNDRED.subtract(others)) + " %).");
        }
    }

    private static String percent(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
