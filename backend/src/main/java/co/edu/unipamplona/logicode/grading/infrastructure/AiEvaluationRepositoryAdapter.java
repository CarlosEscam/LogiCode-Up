package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.application.AiEvaluationRepository;
import co.edu.unipamplona.logicode.grading.domain.AiEvaluation;
import org.springframework.stereotype.Repository;

@Repository
class AiEvaluationRepositoryAdapter implements AiEvaluationRepository {

    private final JpaAiEvaluationRepository jpa;

    AiEvaluationRepositoryAdapter(JpaAiEvaluationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AiEvaluation save(AiEvaluation evaluation) {
        return jpa.save(evaluation);
    }

    @Override
    public List<AiEvaluation> findByGrade(UUID gradeId) {
        return jpa.findByGradeIdOrderByCreatedAtDesc(gradeId);
    }
}
