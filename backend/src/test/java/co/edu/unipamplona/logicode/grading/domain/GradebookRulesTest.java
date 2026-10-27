package co.edu.unipamplona.logicode.grading.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class GradebookRulesTest {

    private static List<BigDecimal> list(String... values) {
        return Arrays.stream(values).map(v -> v == null ? null : new BigDecimal(v)).toList();
    }

    @Test
    void remainingPercentIsSharedEquallyAmongUnweightedActivities() {
        GradebookRules.Weights weights = GradebookRules.weights(list("40", null, null));
        assertThat(weights.effective()).extracting(BigDecimal::doubleValue).containsExactly(40.0, 30.0, 30.0);
        assertThat(weights.complete()).isTrue();
        assertThat(weights.explicitSum()).isEqualByComparingTo("40");
    }

    @Test
    void allWeightedButNotHundredIsIncomplete() {
        assertThat(GradebookRules.weights(list("30", "50")).complete()).isFalse();
        assertThat(GradebookRules.weights(list("30", "70")).complete()).isTrue();
        assertThat(GradebookRules.weights(List.of()).complete()).isFalse();
    }

    @Test
    void definitiveNeedsEveryResultAndPartialUsesOnlyTheKnownOnes() {
        GradebookRules.Weights weights = GradebookRules.weights(list("50", null, null));
        GradebookRules.Totals pending = GradebookRules.totals(weights, list("4.0", "2.0", null));
        // (50 × 4,0 + 25 × 2,0) / 75 = 3,33…
        assertThat(pending.partial()).isEqualByComparingTo("3.3");
        assertThat(pending.definitive()).isNull();

        GradebookRules.Totals done = GradebookRules.totals(weights, list("4.0", "2.0", "0.0"));
        // (200 + 50 + 0) / 100 = 2,5: la vencida sin entrega cuenta como 0,0 (RN-05).
        assertThat(done.partial()).isEqualByComparingTo("2.5");
        assertThat(done.definitive()).isEqualByComparingTo("2.5");
    }

    @Test
    void thirdsRoundToOneDecimalHalfUp() {
        GradebookRules.Weights weights = GradebookRules.weights(list(null, null, null));
        GradebookRules.Totals totals = GradebookRules.totals(weights, list("5.0", "4.5", "4.0"));
        assertThat(totals.definitive()).isEqualByComparingTo("4.5");
        GradebookRules.Totals half = GradebookRules.totals(GradebookRules.weights(list("50", "50")),
                list("3.0", "3.5"));
        assertThat(half.definitive()).isEqualByComparingTo("3.3");
    }

    @Test
    void incompleteWeightsNeverGiveADefinitive() {
        GradebookRules.Weights weights = GradebookRules.weights(list("30", "50"));
        GradebookRules.Totals totals = GradebookRules.totals(weights, list("5.0", "5.0"));
        assertThat(totals.partial()).isEqualByComparingTo("5.0");
        assertThat(totals.definitive()).isNull();
    }

    @Test
    void nothingYetMeansNoPartial() {
        GradebookRules.Weights weights = GradebookRules.weights(list("100"));
        assertThat(GradebookRules.totals(weights, list((String) null)).partial()).isNull();
        assertThat(GradebookRules.passes(new BigDecimal("3.0"))).isTrue();
        assertThat(GradebookRules.passes(new BigDecimal("2.9"))).isFalse();
        assertThat(GradebookRules.average(list("3.0", null, "4.0"))).isEqualByComparingTo("3.5");
    }
}
