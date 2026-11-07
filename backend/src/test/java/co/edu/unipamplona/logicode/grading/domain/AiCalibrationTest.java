package co.edu.unipamplona.logicode.grading.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AiCalibrationTest {

    static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");

    static CalibrationStats.Pair pair(String proposed, String teacher) {
        return new CalibrationStats.Pair(new BigDecimal(proposed), new BigDecimal(teacher));
    }

    @Test
    void agreementUsesTheHalfPointToleranceOnBothSides() {
        CalibrationStats stats = CalibrationStats.of(List.of(pair("4.5", "4.5"), pair("4.0", "4.5"),
                pair("5.0", "4.5"), pair("5.0", "3.0"), pair("1.0", "1.6")));
        assertThat(stats.compared()).isEqualTo(5);
        assertThat(stats.agreeing()).isEqualTo(3);
        assertThat(stats.wouldHaveFailed()).isEqualTo(2);
        assertThat(stats.concordancePercent()).isEqualTo(60);
    }

    @Test
    void withoutComparisonsThereIsNoConcordance() {
        assertThat(CalibrationStats.of(List.of()).concordancePercent()).isNull();
    }

    @Test
    void closesOnlyWithTheMinimumOfComparisonsAndCanBeReopened() {
        AiCalibration calibration = AiCalibration.open(UUID.randomUUID(), NOW);
        assertThat(calibration.isOpen()).isTrue();

        CalibrationStats nine = new CalibrationStats(9, 9, 0);
        assertThatThrownBy(() -> calibration.close(UUID.randomUUID(), nine, 10, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("al menos 10");
        assertThat(calibration.isOpen()).isTrue();

        calibration.close(UUID.randomUUID(), new CalibrationStats(10, 7, 3), 10, NOW);
        assertThat(calibration.isOpen()).isFalse();
        assertThat(calibration.getClosedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> calibration.close(UUID.randomUUID(), new CalibrationStats(10, 10, 0), 10, NOW))
                .isInstanceOf(IllegalStateException.class);

        calibration.reopen(NOW);
        assertThat(calibration.isOpen()).isTrue();
        assertThat(calibration.getClosedAt()).isNull();
    }
}
