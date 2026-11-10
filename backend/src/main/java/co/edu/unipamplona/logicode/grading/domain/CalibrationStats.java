package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Concordancia entre la IA y el docente en un curso (T-90). Solo se comparan las propuestas que la IA habría publicado
 * sola (legibles y con confianza de 90 % o más) contra la nota final que puso el docente. Coinciden si difieren en
 * 0,5 o menos, la misma tolerancia de las pruebas de aceptación (ia-pruebas).
 *
 * @param compared propuestas comparadas
 * @param agreeing comparaciones dentro de la tolerancia
 * @param wouldHaveFailed propuestas que se habrían publicado solas con una nota fuera de la tolerancia
 */
public record CalibrationStats(int compared, int agreeing, int wouldHaveFailed) {

    public static final BigDecimal TOLERANCE = new BigDecimal("0.5");

    /** Propuesta de la IA y nota final del docente para la misma entrega. */
    public record Pair(BigDecimal proposed, BigDecimal teacher) {
    }

    public static CalibrationStats of(List<Pair> pairs) {
        int agreeing = (int) pairs.stream()
                .filter(p -> p.proposed().subtract(p.teacher()).abs().compareTo(TOLERANCE) <= 0).count();
        return new CalibrationStats(pairs.size(), agreeing, pairs.size() - agreeing);
    }

    /** Porcentaje de concordancia, entero hacia abajo; {@code null} sin comparaciones. */
    public Integer concordancePercent() {
        return compared == 0 ? null : agreeing * 100 / compared;
    }
}
