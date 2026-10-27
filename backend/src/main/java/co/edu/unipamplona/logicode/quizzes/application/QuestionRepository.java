package co.edu.unipamplona.logicode.quizzes.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.domain.QuizQuestion;

public interface QuestionRepository {

    QuizQuestion save(QuizQuestion question);

    Optional<QuizQuestion> findById(UUID id);

    List<QuizQuestion> findAllById(Collection<UUID> ids);

    /** Preguntas sin archivar del curso, de la más reciente a la más antigua. */
    List<QuizQuestion> findActiveByCourse(UUID courseId);
}
