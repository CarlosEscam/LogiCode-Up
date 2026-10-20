package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.grading.domain.RubricCriterion;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RN-20: el docente define la respuesta esperada y los criterios de la rúbrica. Cada cambio guarda una versión nueva
 * con el enunciado vigente; las entregas anteriores conservan la suya.
 */
@Service
public class ManageRubrics {

    public record CriterionInput(String name, String description, BigDecimal maxPoints) {
    }

    private static final BigDecimal MAX_POINTS = new BigDecimal("1000");

    private final RubricRepository rubrics;
    private final ActivityCatalog catalog;
    private final GradingAccess access;
    private final Clock clock;

    public ManageRubrics(RubricRepository rubrics, ActivityCatalog catalog, GradingAccess access, Clock clock) {
        this.rubrics = rubrics;
        this.catalog = catalog;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public RubricView define(Actor actor, UUID activityId, String expectedAnswer, List<CriterionInput> criteria) {
        ActivityCatalog.ActivityInfo activity = catalog.requireManageable(actor, activityId);
        List<RubricCriterion> valid = validate(criteria);
        int number = rubrics.findCurrent(activityId).map(RubricVersion::getNumber).orElse(0) + 1;
        RubricVersion saved = rubrics.save(new RubricVersion(activityId, number, activity.statement(),
                clean(expectedAnswer), valid, actor.userId(), clock.instant()));
        return GradingViews.rubric(saved, true);
    }

    /** Rúbrica vigente. El estudiante ve los criterios, no la respuesta esperada. */
    @Transactional(readOnly = true)
    public Optional<RubricView> current(Actor actor, UUID activityId) {
        ActivityCatalog.ActivityInfo activity = catalog.requireVisible(actor, activityId);
        boolean full = access.canViewCourseWork(actor, activity.courseId());
        return rubrics.findCurrent(activityId).map(r -> GradingViews.rubric(r, full));
    }

    /** Todas las versiones, para el docente y quien ve las entregas del curso. */
    @Transactional(readOnly = true)
    public List<RubricView> versions(Actor actor, UUID activityId) {
        ActivityCatalog.ActivityInfo activity = catalog.requireVisible(actor, activityId);
        access.requireViewCourseWork(actor, activity.courseId());
        return rubrics.findByActivity(activityId).stream().map(r -> GradingViews.rubric(r, true)).toList();
    }

    private static List<RubricCriterion> validate(List<CriterionInput> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw ApplicationException.businessRule("La rúbrica necesita por lo menos un criterio.");
        }
        if (criteria.size() > RubricVersion.MAX_CRITERIA) {
            throw ApplicationException.businessRule("La rúbrica admite hasta " + RubricVersion.MAX_CRITERIA
                    + " criterios.");
        }
        return criteria.stream().map(c -> {
            String name = clean(c.name());
            if (name == null) {
                throw ApplicationException.businessRule("Cada criterio necesita un nombre.");
            }
            BigDecimal points = c.maxPoints();
            if (points == null || points.signum() <= 0 || points.compareTo(MAX_POINTS) > 0
                    || points.stripTrailingZeros().scale() > 2) {
                throw ApplicationException.businessRule("El puntaje de «" + name
                        + "» debe ser mayor que 0 y como máximo 1000, con hasta dos decimales.");
            }
            return new RubricCriterion(name, clean(c.description()), points);
        }).toList();
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
