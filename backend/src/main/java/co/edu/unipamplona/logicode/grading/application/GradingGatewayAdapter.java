package co.edu.unipamplona.logicode.grading.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.GradingGateway;
import co.edu.unipamplona.logicode.grading.domain.AiGradingJob;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.ReviewReason;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import org.springframework.stereotype.Component;

/**
 * Implementa el puerto de activities: rúbrica obligatoria, una nota por entrega (en cola de IA o en Por revisar) y
 * versiones de rúbrica por cambio de enunciado.
 */
@Component
class GradingGatewayAdapter implements GradingGateway {

    private final RubricRepository rubrics;
    private final GradeRepository grades;
    private final AiGradingJobRepository jobs;
    private final AiSubmissionContent content;
    private final AiGradingSettings settings;
    private final Clock clock;

    GradingGatewayAdapter(RubricRepository rubrics, GradeRepository grades, AiGradingJobRepository jobs,
            AiSubmissionContent content, AiGradingSettings settings, Clock clock) {
        this.rubrics = rubrics;
        this.grades = grades;
        this.jobs = jobs;
        this.content = content;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public boolean hasRubric(UUID activityId) {
        return rubrics.findCurrent(activityId).isPresent();
    }

    /**
     * La entrega queda con la rúbrica vigente y entra a la cola de IA en la misma transacción (RNF-07: nunca se
     * pierde ni bloquea la entrega). Sin IA o con formatos que la IA no lee, pasa directo a Por revisar.
     */
    @Override
    public void submissionReceived(UUID submissionId, UUID activityId) {
        RubricVersion current = rubrics.findCurrent(activityId)
                .orElseThrow(() -> new IllegalStateException("Actividad publicada sin rúbrica: " + activityId));
        Instant now = clock.instant();
        if (!settings.enabled()) {
            grades.save(Grade.forReview(submissionId, current.getId(), ReviewReason.AI_DISABLED, now));
        } else if (!content.isReadable(submissionId)) {
            grades.save(Grade.forReview(submissionId, current.getId(), ReviewReason.UNSUPPORTED, now));
        } else {
            Grade grade = grades.save(Grade.forAi(submissionId, current.getId(), now));
            jobs.save(new AiGradingJob(grade.getId(), null, now));
        }
    }

    @Override
    public void statementChanged(UUID activityId, String statement, UUID changedBy) {
        rubrics.findCurrent(activityId)
                .ifPresent(current -> rubrics.save(current.withStatement(statement, changedBy, clock.instant())));
    }
}
