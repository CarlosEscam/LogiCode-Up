package co.edu.unipamplona.logicode.quizzes.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.application.QuestionRepository;
import co.edu.unipamplona.logicode.quizzes.domain.QuizQuestion;
import org.springframework.stereotype.Repository;

@Repository
class QuestionRepositoryAdapter implements QuestionRepository {

    private final JpaQuestionRepository jpa;

    QuestionRepositoryAdapter(JpaQuestionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public QuizQuestion save(QuizQuestion question) {
        return jpa.save(question);
    }

    @Override
    public Optional<QuizQuestion> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<QuizQuestion> findAllById(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllById(ids);
    }

    @Override
    public List<QuizQuestion> findActiveByCourse(UUID courseId) {
        return jpa.findByCourseIdAndArchivedAtIsNullOrderByCreatedAtDesc(courseId);
    }
}
