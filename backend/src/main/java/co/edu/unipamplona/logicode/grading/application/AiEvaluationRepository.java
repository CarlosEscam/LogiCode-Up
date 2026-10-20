package co.edu.unipamplona.logicode.grading.application;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiEvaluation;

public interface AiEvaluationRepository {

    AiEvaluation save(AiEvaluation evaluation);

    /** Evaluaciones de la nota, de la más reciente a la más antigua. */
    List<AiEvaluation> findByGrade(UUID gradeId);
}
