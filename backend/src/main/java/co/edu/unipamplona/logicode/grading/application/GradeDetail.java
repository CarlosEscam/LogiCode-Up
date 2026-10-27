package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import co.edu.unipamplona.logicode.grading.domain.GradeSource;

/**
 * Nota de una entrega con la versión de rúbrica con que se evalúa. El historial, las evaluaciones con IA (de la más
 * reciente a la más antigua) y la respuesta esperada solo los ve quien consulta las entregas del curso.
 */
public record GradeDetail(GradeView grade, RubricView rubric, List<Change> history,
        List<AiEvaluationView> aiEvaluations, boolean canGrade, boolean canRetryWithAi, boolean canRunTests) {

    public record Change(BigDecimal previousScore, BigDecimal newScore, String feedback, GradeSource source,
            String changedByName, Instant changedAt) {
    }
}
