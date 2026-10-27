package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Reglas de la planilla (RN-07, RN-06). Las actividades con porcentaje pesan ese porcentaje; el resto se reparte por
 * igual entre las que no lo tienen. La nota definitiva solo existe cuando los pesos suman 100 % y todas las
 * actividades tienen resultado; el avance parcial promedia, con sus pesos, las que ya lo tienen.
 */
public final class GradebookRules {

    public static final BigDecimal HUNDRED = new BigDecimal("100");
    /** Nota mínima aprobatoria de la escala 0,0 a 5,0 (T-61). */
    public static final BigDecimal PASSING_SCORE = new BigDecimal("3.0");
    private static final int SCALE = 8;

    private GradebookRules() {
    }

    /**
     * Pesos efectivos, en el mismo orden. {@code complete} es falso si no hay actividades o si todas tienen porcentaje
     * y no suman 100 %: el docente debe ajustarlos antes de cerrar la definitiva.
     */
    public record Weights(List<BigDecimal> effective, BigDecimal explicitSum, boolean complete) {
    }

    /** Avance parcial y nota definitiva con un decimal; nulos si aún no se pueden calcular. */
    public record Totals(BigDecimal partial, BigDecimal definitive) {
    }

    public static Weights weights(List<BigDecimal> explicitWeights) {
        BigDecimal explicitSum = explicitWeights.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        long unweighted = explicitWeights.stream().filter(Objects::isNull).count();
        BigDecimal share = unweighted == 0 ? BigDecimal.ZERO
                : HUNDRED.subtract(explicitSum).max(BigDecimal.ZERO)
                        .divide(BigDecimal.valueOf(unweighted), SCALE, RoundingMode.HALF_EVEN);
        List<BigDecimal> effective = explicitWeights.stream().map(w -> w == null ? share : w).toList();
        boolean complete = !explicitWeights.isEmpty() && explicitSum.compareTo(HUNDRED) <= 0
                && (unweighted > 0 || explicitSum.compareTo(HUNDRED) == 0);
        return new Weights(effective, explicitSum, complete);
    }

    /** {@code results}: nota de cada actividad (0,0 si venció sin entrega) o nulo si aún no tiene resultado. */
    public static Totals totals(Weights weights, List<BigDecimal> results) {
        if (results.size() != weights.effective().size()) {
            throw new IllegalArgumentException("Cada actividad necesita su resultado");
        }
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weightWithResult = BigDecimal.ZERO;
        boolean allResults = true;
        for (int i = 0; i < results.size(); i++) {
            BigDecimal result = results.get(i);
            if (result == null) {
                allResults = false;
                continue;
            }
            weighted = weighted.add(weights.effective().get(i).multiply(result));
            weightWithResult = weightWithResult.add(weights.effective().get(i));
        }
        BigDecimal partial = weightWithResult.signum() == 0 ? null
                : round(weighted.divide(weightWithResult, SCALE, RoundingMode.HALF_EVEN));
        BigDecimal definitive = weights.complete() && allResults
                ? round(weighted.divide(HUNDRED, SCALE, RoundingMode.HALF_EVEN)) : null;
        return new Totals(partial, definitive);
    }

    /** Promedio simple con un decimal, o nulo si no hay valores. */
    public static BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return round(present.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(present.size()), SCALE, RoundingMode.HALF_EVEN));
    }

    public static boolean passes(BigDecimal score) {
        return score != null && score.compareTo(PASSING_SCORE) >= 0;
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }
}
