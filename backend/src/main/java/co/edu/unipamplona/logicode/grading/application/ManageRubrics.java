package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.grading.domain.RubricCriterion;
import co.edu.unipamplona.logicode.grading.domain.RubricTestCase;
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

    public record TestCaseInput(String input, String expectedOutput) {
    }

    private static final BigDecimal MAX_POINTS = new BigDecimal("1000");
    private static final int MAX_CASE_CHARS = 5000;

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
    public RubricView define(Actor actor, UUID activityId, String expectedAnswer, List<CriterionInput> criteria,
            List<TestCaseInput> testCases) {
        ActivityCatalog.ActivityInfo activity = catalog.requireManageable(actor, activityId);
        if (activity.quiz()) {
            throw ApplicationException.businessRule("La rúbrica de un quiz se arma con sus preguntas.");
        }
        return GradingViews.rubric(save(activity, expectedAnswer, validate(criteria), validateCases(testCases),
                actor.userId()), true);
    }

    /**
     * RN-20 para quizzes: el módulo quizzes guarda como rúbrica las preguntas (un criterio por pregunta) y la clave
     * de respuestas, después de verificar que el actor administra el quiz.
     */
    @Transactional
    public void defineForQuiz(UUID activityId, String answerKey, List<CriterionInput> questions, UUID definedBy) {
        ActivityCatalog.ActivityInfo activity = catalog.activity(activityId)
                .orElseThrow(() -> ApplicationException.notFound("La actividad no existe."));
        save(activity, answerKey, validate(questions), List.of(), definedBy);
    }

    private RubricVersion save(ActivityCatalog.ActivityInfo activity, String expectedAnswer,
            List<RubricCriterion> criteria, List<RubricTestCase> testCases, UUID by) {
        int number = rubrics.findCurrent(activity.id()).map(RubricVersion::getNumber).orElse(0) + 1;
        return rubrics.save(new RubricVersion(activity.id(), number, activity.statement(), clean(expectedAnswer),
                criteria, testCases, by, clock.instant()));
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

    private static List<RubricTestCase> validateCases(List<TestCaseInput> cases) {
        if (cases == null) {
            return List.of();
        }
        if (cases.size() > RubricVersion.MAX_TEST_CASES) {
            throw ApplicationException.businessRule("La rúbrica admite hasta " + RubricVersion.MAX_TEST_CASES
                    + " casos de prueba.");
        }
        return cases.stream().map(c -> {
            String expected = clean(c.expectedOutput());
            String input = c.input() == null ? "" : c.input().replace("\r", "");
            if (expected == null) {
                throw ApplicationException.businessRule("Cada caso de prueba necesita la salida esperada.");
            }
            if (expected.length() > MAX_CASE_CHARS || input.length() > MAX_CASE_CHARS) {
                throw ApplicationException.businessRule("Las entradas y salidas de un caso admiten hasta "
                        + MAX_CASE_CHARS + " caracteres.");
            }
            return new RubricTestCase(input, expected.replace("\r", ""));
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
