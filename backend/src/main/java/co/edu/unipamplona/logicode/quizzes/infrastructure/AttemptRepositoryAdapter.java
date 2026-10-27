package co.edu.unipamplona.logicode.quizzes.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.application.AttemptRepository;
import co.edu.unipamplona.logicode.quizzes.domain.AttemptStatus;
import co.edu.unipamplona.logicode.quizzes.domain.QuizAttempt;
import org.springframework.stereotype.Repository;

@Repository
class AttemptRepositoryAdapter implements AttemptRepository {

    private final JpaAttemptRepository jpa;

    AttemptRepositoryAdapter(JpaAttemptRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public QuizAttempt save(QuizAttempt attempt) {
        return jpa.save(attempt);
    }

    @Override
    public Optional<QuizAttempt> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<QuizAttempt> findByActivityAndStudent(UUID activityId, UUID studentId) {
        return jpa.findByActivityIdAndStudentIdOrderByNumberAsc(activityId, studentId);
    }

    @Override
    public List<QuizAttempt> findFinishedByActivity(UUID activityId) {
        return jpa.findByActivityIdAndStatus(activityId, AttemptStatus.FINISHED);
    }

    @Override
    public boolean existsByActivity(UUID activityId) {
        return jpa.existsByActivityId(activityId);
    }

    @Override
    public List<QuizAttempt> findOpenWithDeadlineBefore(Instant cutoff) {
        return jpa.findByStatusAndDeadlineBefore(AttemptStatus.IN_PROGRESS, cutoff);
    }
}
