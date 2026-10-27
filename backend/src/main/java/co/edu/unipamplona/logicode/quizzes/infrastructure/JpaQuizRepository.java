package co.edu.unipamplona.logicode.quizzes.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.quizzes.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaQuizRepository extends JpaRepository<Quiz, UUID> {

    @Query("select distinct q from Quiz q join q.questionIds questionId where questionId = :questionId")
    List<Quiz> findUsing(@Param("questionId") UUID questionId);
}
