package co.edu.unipamplona.logicode.quizzes.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByCourseIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID courseId);
}
