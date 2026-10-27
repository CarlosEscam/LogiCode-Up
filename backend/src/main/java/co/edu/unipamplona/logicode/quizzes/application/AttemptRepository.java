package co.edu.unipamplona.logicode.quizzes.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.domain.QuizAttempt;

public interface AttemptRepository {

    QuizAttempt save(QuizAttempt attempt);

    Optional<QuizAttempt> findById(UUID id);

    /** Intentos del estudiante en el quiz, del primero al último. */
    List<QuizAttempt> findByActivityAndStudent(UUID activityId, UUID studentId);

    List<QuizAttempt> findFinishedByActivity(UUID activityId);

    boolean existsByActivity(UUID activityId);

    /** Intentos en curso cuyo plazo terminó antes de {@code cutoff}. */
    List<QuizAttempt> findOpenWithDeadlineBefore(Instant cutoff);
}
