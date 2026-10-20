package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Versión de rúbrica. {@code expectedAnswer} es nulo para quien no ve las entregas del curso (el estudiante). */
public record RubricView(UUID id, UUID activityId, int number, String statement, String expectedAnswer,
        List<Criterion> criteria, BigDecimal totalPoints, Instant createdAt) {

    public record Criterion(String name, String description, BigDecimal maxPoints) {
    }
}
