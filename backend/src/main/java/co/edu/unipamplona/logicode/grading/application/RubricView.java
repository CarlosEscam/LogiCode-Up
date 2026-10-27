package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Versión de rúbrica. {@code expectedAnswer} y {@code testCases} son nulos para quien no ve las entregas del curso
 * (el estudiante), que solo sabe cuántos casos de prueba hay.
 */
public record RubricView(UUID id, UUID activityId, int number, String statement, String expectedAnswer,
        List<Criterion> criteria, BigDecimal totalPoints, List<TestCase> testCases, int testCaseCount,
        Instant createdAt) {

    public record Criterion(String name, String description, BigDecimal maxPoints) {
    }

    public record TestCase(String input, String expectedOutput) {
    }
}
