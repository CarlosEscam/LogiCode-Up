package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaAiEvaluationRepository extends JpaRepository<AiEvaluation, UUID> {

    List<AiEvaluation> findByGradeIdOrderByCreatedAtDesc(UUID gradeId);
}
