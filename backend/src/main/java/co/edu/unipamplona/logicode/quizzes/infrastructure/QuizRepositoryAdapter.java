package co.edu.unipamplona.logicode.quizzes.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.application.QuizRepository;
import co.edu.unipamplona.logicode.quizzes.domain.Quiz;
import org.springframework.stereotype.Repository;

@Repository
class QuizRepositoryAdapter implements QuizRepository {

    private final JpaQuizRepository jpa;

    QuizRepositoryAdapter(JpaQuizRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Quiz save(Quiz quiz) {
        return jpa.save(quiz);
    }

    @Override
    public Optional<Quiz> findById(UUID activityId) {
        return jpa.findById(activityId);
    }

    @Override
    public List<Quiz> findUsing(UUID questionId) {
        return jpa.findUsing(questionId);
    }
}
