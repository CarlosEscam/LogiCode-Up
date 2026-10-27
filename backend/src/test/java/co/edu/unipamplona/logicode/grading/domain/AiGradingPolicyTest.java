package co.edu.unipamplona.logicode.grading.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** RF-14, RF-15, RN-09, RN-11: publicación automática solo con contenido legible y confianza de 80 % o más. */
class AiGradingPolicyTest {

    static final RubricVersion RUBRIC = new RubricVersion(UUID.randomUUID(), 1, "Enunciado", "Esperada",
            List.of(new RubricCriterion("Lectura", null, new BigDecimal("6")),
                    new RubricCriterion("Cálculo", null, new BigDecimal("4"))),
            List.of(), UUID.randomUUID(), Instant.parse("2026-10-20T15:00:00Z"));

    @Test
    void legibleWithEightyPercentConfidencePublishes() {
        AiGradingPolicy.Decision decision = AiGradingPolicy.decide(RUBRIC, true, 80, points("6", "3"));
        assertThat(decision.outcome()).isEqualTo(AiOutcome.PUBLISHED);
        assertThat(decision.publishes()).isTrue();
        assertThat(decision.score()).isEqualByComparingTo("4.5");
        assertThat(decision.criteria()).extracting(AiCriterionScore::getName).containsExactly("Lectura", "Cálculo");
    }

    @Test
    void belowEightyPercentGoesToTheTeacherWithAProposal() {
        AiGradingPolicy.Decision decision = AiGradingPolicy.decide(RUBRIC, true, 79, points("5.5", "2"));
        assertThat(decision.outcome()).isEqualTo(AiOutcome.LOW_CONFIDENCE);
        assertThat(decision.reviewReason()).isEqualTo(ReviewReason.LOW_CONFIDENCE);
        assertThat(decision.score()).isEqualByComparingTo("3.8");
    }

    @Test
    void illegibleContentNeverGetsAScore() {
        AiGradingPolicy.Decision decision = AiGradingPolicy.decide(RUBRIC, false, 99, List.of());
        assertThat(decision.outcome()).isEqualTo(AiOutcome.ILLEGIBLE);
        assertThat(decision.reviewReason()).isEqualTo(ReviewReason.ILLEGIBLE);
        assertThat(decision.score()).isNull();
        assertThat(decision.criteria()).isEmpty();
    }

    @Test
    void answersThatDoNotMatchTheRubricAreRejected() {
        assertThatThrownBy(() -> AiGradingPolicy.decide(RUBRIC, true, 90, points("6")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiGradingPolicy.decide(RUBRIC, true, 90, points("7", "1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiGradingPolicy.decide(RUBRIC, true, 90, points("-1", "1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiGradingPolicy.decide(RUBRIC, true, 101, points("1", "1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoreIsRoundedToOneDecimalOnTheZeroToFiveScale() {
        assertThat(AiGradingPolicy.scoreFor(new BigDecimal("7"), new BigDecimal("9"))).isEqualByComparingTo("3.9");
        assertThat(AiGradingPolicy.scoreFor(BigDecimal.ZERO, new BigDecimal("10"))).isEqualByComparingTo("0.0");
        assertThat(AiGradingPolicy.scoreFor(new BigDecimal("10"), new BigDecimal("10"))).isEqualByComparingTo("5.0");
    }

    private static List<AiGradingPolicy.CriterionResult> points(String... values) {
        return List.of(values).stream().map(v -> new AiGradingPolicy.CriterionResult(new BigDecimal(v), " ok ")).toList();
    }
}
