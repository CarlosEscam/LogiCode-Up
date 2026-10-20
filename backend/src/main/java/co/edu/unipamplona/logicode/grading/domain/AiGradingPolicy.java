package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Regla de publicación automática (RF-14, RF-15, RN-09, RN-11): con contenido legible y confianza reportada igual o
 * superior al 80 % se publica la nota; si no, pasa a confirmación docente. La nota se calcula con los puntos que el
 * modelo dio a cada criterio de la rúbrica, llevados a la escala 0,0 a 5,0. Una respuesta incoherente con la rúbrica
 * no es una nota: se rechaza como fallo técnico.
 */
public final class AiGradingPolicy {

    public static final int AUTO_PUBLISH_CONFIDENCE = 80;

    /** Lo que el modelo devolvió para un criterio, en el orden de la rúbrica. */
    public record CriterionResult(BigDecimal points, String comment) {
    }

    /** Decisión: {@code score} y {@code criteria} solo existen cuando el contenido es legible. */
    public record Decision(AiOutcome outcome, boolean legible, int confidence, BigDecimal score,
            List<AiCriterionScore> criteria) {

        public boolean publishes() {
            return outcome == AiOutcome.PUBLISHED;
        }

        public ReviewReason reviewReason() {
            return switch (outcome) {
                case ILLEGIBLE -> ReviewReason.ILLEGIBLE;
                case LOW_CONFIDENCE -> ReviewReason.LOW_CONFIDENCE;
                default -> throw new IllegalStateException("La decisión " + outcome + " no pasa a revisión");
            };
        }
    }

    private AiGradingPolicy() {
    }

    /** @throws IllegalArgumentException si la respuesta no corresponde a la rúbrica (cuenta como fallo técnico) */
    public static Decision decide(RubricVersion rubric, boolean legible, int confidence,
            List<CriterionResult> results) {
        if (confidence < 0 || confidence > 100) {
            throw new IllegalArgumentException("La confianza reportada está fuera de 0 a 100: " + confidence);
        }
        if (!legible) {
            return new Decision(AiOutcome.ILLEGIBLE, false, confidence, null, List.of());
        }
        List<RubricCriterion> criteria = rubric.getCriteria();
        if (results.size() != criteria.size()) {
            throw new IllegalArgumentException("El modelo evaluó " + results.size() + " criterios y la rúbrica tiene "
                    + criteria.size());
        }
        List<AiCriterionScore> scored = new ArrayList<>();
        BigDecimal earned = BigDecimal.ZERO;
        for (int i = 0; i < criteria.size(); i++) {
            RubricCriterion criterion = criteria.get(i);
            BigDecimal points = results.get(i).points();
            if (points == null || points.signum() < 0 || points.compareTo(criterion.getMaxPoints()) > 0) {
                throw new IllegalArgumentException("Puntos fuera de rango en el criterio \"" + criterion.getName()
                        + "\": " + points + " de " + criterion.getMaxPoints());
            }
            BigDecimal normalized = points.setScale(2, RoundingMode.HALF_UP);
            scored.add(new AiCriterionScore(criterion.getName(), normalized, criterion.getMaxPoints(),
                    blankToNull(results.get(i).comment())));
            earned = earned.add(normalized);
        }
        BigDecimal score = scoreFor(earned, rubric.totalPoints());
        AiOutcome outcome = confidence >= AUTO_PUBLISH_CONFIDENCE ? AiOutcome.PUBLISHED : AiOutcome.LOW_CONFIDENCE;
        return new Decision(outcome, true, confidence, score, scored);
    }

    /** Puntos obtenidos sobre el total de la rúbrica, en la escala 0,0 a 5,0 con un decimal. */
    public static BigDecimal scoreFor(BigDecimal earned, BigDecimal total) {
        return earned.multiply(Grade.MAX_SCORE).divide(total, 1, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
