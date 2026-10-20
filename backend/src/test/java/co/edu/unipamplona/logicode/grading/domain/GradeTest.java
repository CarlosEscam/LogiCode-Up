package co.edu.unipamplona.logicode.grading.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** RN-06: escala de 0,0 a 5,0 con un decimal; cada publicación deja su cambio. */
class GradeTest {

    static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

    @Test
    void validScoresUseOneDecimalBetweenZeroAndFive() {
        assertThat(Grade.isValidScore(new BigDecimal("0"))).isTrue();
        assertThat(Grade.isValidScore(new BigDecimal("5.0"))).isTrue();
        assertThat(Grade.isValidScore(new BigDecimal("3.50"))).isTrue();
        assertThat(Grade.isValidScore(new BigDecimal("3.55"))).isFalse();
        assertThat(Grade.isValidScore(new BigDecimal("5.1"))).isFalse();
        assertThat(Grade.isValidScore(new BigDecimal("-0.1"))).isFalse();
        assertThat(Grade.isValidScore(null)).isFalse();
    }

    @Test
    void publishingAndCorrectingRecordsPreviousScore() {
        Grade grade = new Grade(UUID.randomUUID(), UUID.randomUUID(), NOW);
        assertThat(grade.getStatus()).isEqualTo(GradeStatus.PENDING_REVIEW);

        GradeChange first = grade.publish(new BigDecimal("4"), "Bien", GradeSource.TEACHER, UUID.randomUUID(), NOW);
        assertThat(grade.isPublished()).isTrue();
        assertThat(grade.getScore()).isEqualByComparingTo("4.0");
        assertThat(first.getPreviousScore()).isNull();

        GradeChange second = grade.publish(new BigDecimal("3.5"), null, GradeSource.TEACHER, UUID.randomUUID(), NOW);
        assertThat(second.getPreviousScore()).isEqualByComparingTo("4.0");
        assertThat(second.getNewScore()).isEqualByComparingTo("3.5");
    }

    @Test
    void rejectsScoresOutsideTheScale() {
        Grade grade = new Grade(UUID.randomUUID(), UUID.randomUUID(), NOW);
        assertThatThrownBy(() -> grade.publish(new BigDecimal("6"), null, GradeSource.TEACHER, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
