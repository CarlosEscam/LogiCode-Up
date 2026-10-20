package co.edu.unipamplona.logicode.activities.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.ActivityKind;
import co.edu.unipamplona.logicode.activities.domain.DeadlineException;
import co.edu.unipamplona.logicode.activities.domain.Submission;
import org.junit.jupiter.api.Test;

/** RN-05: estados del estudiante según apertura, cierre, intentos y excepción de plazo. */
class StudentProgressTest {

    static final Instant OPENS = Instant.parse("2026-10-01T13:00:00Z");
    static final Instant CLOSES = Instant.parse("2026-10-08T04:59:00Z");
    static final UUID STUDENT = UUID.randomUUID();

    final Activity activity = published(2);

    @Test
    void notOpenBeforeOpeningDate() {
        var progress = ActivityViews.progress(activity, List.of(), null, OPENS.minusSeconds(1));
        assertThat(progress.state()).isEqualTo(ActivityView.State.NOT_OPEN);
        assertThat(progress.canSubmit()).isFalse();
    }

    @Test
    void openUntilTheClosingInstantInclusive() {
        var progress = ActivityViews.progress(activity, List.of(), null, CLOSES);
        assertThat(progress.state()).isEqualTo(ActivityView.State.OPEN);
        assertThat(progress.canSubmit()).isTrue();
        assertThat(progress.attemptsAllowed()).isEqualTo(2);
    }

    @Test
    void overdueWithoutSubmissionAfterClosing() {
        var progress = ActivityViews.progress(activity, List.of(), null, CLOSES.plusSeconds(1));
        assertThat(progress.state()).isEqualTo(ActivityView.State.OVERDUE);
        assertThat(progress.canSubmit()).isFalse();
    }

    @Test
    void submittedKeepsAttemptsLeftUntilClosing() {
        Submission first = submission(1, OPENS.plus(Duration.ofDays(1)));
        var open = ActivityViews.progress(activity, List.of(first), null, OPENS.plus(Duration.ofDays(2)));
        assertThat(open.state()).isEqualTo(ActivityView.State.SUBMITTED);
        assertThat(open.canSubmit()).isTrue();
        assertThat(open.lastSubmissionId()).isEqualTo(first.getId());

        var full = ActivityViews.progress(activity, List.of(first, submission(2, OPENS.plus(Duration.ofDays(2)))),
                null, OPENS.plus(Duration.ofDays(3)));
        assertThat(full.canSubmit()).isFalse();
    }

    @Test
    void exceptionExtendsDeadlineAndAddsAttempts() {
        DeadlineException exception = new DeadlineException(activity.getId(), STUDENT);
        Instant extended = CLOSES.plus(Duration.ofDays(2));
        exception.grant(extended, 1, "Incapacidad", UUID.randomUUID(), CLOSES);

        var progress = ActivityViews.progress(activity, List.of(), exception, CLOSES.plus(Duration.ofDays(1)));
        assertThat(progress.state()).isEqualTo(ActivityView.State.OPEN);
        assertThat(progress.deadline()).isEqualTo(extended);
        assertThat(progress.attemptsAllowed()).isEqualTo(3);
        assertThat(progress.hasException()).isTrue();
    }

    @Test
    void earlierExtensionNeverShortensTheDeadline() {
        DeadlineException exception = new DeadlineException(activity.getId(), STUDENT);
        exception.grant(CLOSES.minus(Duration.ofDays(1)), 1, null, UUID.randomUUID(), OPENS);
        assertThat(ActivityViews.progress(activity, List.of(), exception, OPENS).deadline()).isEqualTo(CLOSES);
    }

    @Test
    void draftNeverAcceptsSubmissions() {
        Activity draft = new Activity(UUID.randomUUID(), definition(1), UUID.randomUUID(), OPENS);
        assertThat(ActivityViews.progress(draft, List.of(), null, OPENS.plusSeconds(60)).canSubmit()).isFalse();
    }

    private static Activity published(int attempts) {
        Activity activity = new Activity(UUID.randomUUID(), definition(attempts), UUID.randomUUID(), OPENS);
        activity.publish(OPENS);
        return activity;
    }

    private static Activity.Definition definition(int attempts) {
        return new Activity.Definition("Promedio", ActivityKind.TASK, "Calcula el promedio.", true, false, false,
                Set.of(), null, OPENS, CLOSES, attempts);
    }

    private Submission submission(int attempt, Instant at) {
        return new Submission(activity.getId(), STUDENT, attempt, "respuesta", List.of(), false, at);
    }
}
