package co.edu.unipamplona.logicode.quizzes.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.domain.AttemptStatus;
import co.edu.unipamplona.logicode.quizzes.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByActivityIdAndStudentIdOrderByNumberAsc(UUID activityId, UUID studentId);

    List<QuizAttempt> findByActivityIdAndStatus(UUID activityId, AttemptStatus status);

    boolean existsByActivityId(UUID activityId);

    List<QuizAttempt> findByStatusAndDeadlineBefore(AttemptStatus status, Instant cutoff);
}
