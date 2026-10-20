package co.edu.unipamplona.logicode.grading.application;

import java.time.Clock;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.GradingGateway;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import org.springframework.stereotype.Component;

/** Implementa el puerto de activities: rúbrica obligatoria, nota pendiente por entrega y versiones por enunciado. */
@Component
class GradingGatewayAdapter implements GradingGateway {

    private final RubricRepository rubrics;
    private final GradeRepository grades;
    private final Clock clock;

    GradingGatewayAdapter(RubricRepository rubrics, GradeRepository grades, Clock clock) {
        this.rubrics = rubrics;
        this.grades = grades;
        this.clock = clock;
    }

    @Override
    public boolean hasRubric(UUID activityId) {
        return rubrics.findCurrent(activityId).isPresent();
    }

    /** La entrega entra a Por revisar con la rúbrica vigente. La calificación con IA llega en la Etapa 6. */
    @Override
    public void submissionReceived(UUID submissionId, UUID activityId) {
        RubricVersion current = rubrics.findCurrent(activityId)
                .orElseThrow(() -> new IllegalStateException("Actividad publicada sin rúbrica: " + activityId));
        grades.save(new Grade(submissionId, current.getId(), clock.instant()));
    }

    @Override
    public void statementChanged(UUID activityId, String statement, UUID changedBy) {
        rubrics.findCurrent(activityId)
                .ifPresent(current -> rubrics.save(current.withStatement(statement, changedBy, clock.instant())));
    }
}
