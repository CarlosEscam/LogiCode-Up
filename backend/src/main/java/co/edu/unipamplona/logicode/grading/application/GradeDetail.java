package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import co.edu.unipamplona.logicode.grading.domain.GradeSource;

/**
 * Nota de una entrega con la versión de rúbrica con que se evalúa. El historial y la respuesta esperada solo los ve
 * quien consulta las entregas del curso.
 */
public record GradeDetail(GradeView grade, RubricView rubric, List<Change> history, boolean canGrade) {

    public record Change(BigDecimal previousScore, BigDecimal newScore, String feedback, GradeSource source,
            String changedByName, Instant changedAt) {
    }
}
