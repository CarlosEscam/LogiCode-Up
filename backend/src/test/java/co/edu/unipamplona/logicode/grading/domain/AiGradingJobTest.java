package co.edu.unipamplona.logicode.grading.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** RNF-06: reintentos controlados y una sola toma vigente por trabajo. */
class AiGradingJobTest {

    static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

    @Test
    void failuresRetryWithGrowingDelayUntilAttemptsRunOut() {
        AiGradingJob job = new AiGradingJob(UUID.randomUUID(), null, NOW);
        job.claim(NOW);
        assertThat(job.fail("caído", 2, Duration.ofMinutes(2), NOW)).isTrue();
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.PENDING);
        assertThat(job.getNextRunAt()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));

        job.claim(NOW);
        assertThat(job.fail("caído", 2, Duration.ofMinutes(2), NOW)).isFalse();
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(job.getLastError()).isEqualTo("caído");
    }

    @Test
    void onlyTheCurrentClaimCanFinishTheJob() {
        AiGradingJob job = new AiGradingJob(UUID.randomUUID(), null, NOW);
        UUID first = job.claim(NOW);
        UUID second = job.claim(NOW.plus(Duration.ofMinutes(15)));
        assertThat(job.isClaimedBy(first)).isFalse();
        assertThat(job.isClaimedBy(second)).isTrue();
        assertThat(job.getAttempts()).isEqualTo(2);
        job.succeed(NOW);
        assertThat(job.isClaimedBy(second)).isFalse();
    }

    @Test
    void gradeMovesFromAiToReviewAndPublishing() {
        Grade grade = Grade.forAi(UUID.randomUUID(), UUID.randomUUID(), NOW);
        assertThat(grade.isProcessing()).isTrue();
        grade.sendToReview(ReviewReason.LOW_CONFIDENCE, NOW);
        assertThat(grade.getStatus()).isEqualTo(GradeStatus.PENDING_REVIEW);
        assertThat(grade.getReviewReason()).isEqualTo(ReviewReason.LOW_CONFIDENCE);
        grade.startAiEvaluation(NOW);
        assertThat(grade.getReviewReason()).isNull();
        grade.publish(new java.math.BigDecimal("4.0"), null, GradeSource.AI, null, NOW);
        assertThat(grade.isPublished()).isTrue();
    }
}
