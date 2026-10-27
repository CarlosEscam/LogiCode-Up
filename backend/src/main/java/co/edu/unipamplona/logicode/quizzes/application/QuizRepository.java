package co.edu.unipamplona.logicode.quizzes.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.domain.Quiz;

public interface QuizRepository {

    Quiz save(Quiz quiz);

    Optional<Quiz> findById(UUID activityId);

    /** Quizzes que usan la pregunta. */
    List<Quiz> findUsing(UUID questionId);
}
