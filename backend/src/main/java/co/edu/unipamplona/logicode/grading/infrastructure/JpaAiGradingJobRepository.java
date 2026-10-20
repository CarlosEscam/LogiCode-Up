package co.edu.unipamplona.logicode.grading.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiGradingJob;
import co.edu.unipamplona.logicode.grading.domain.AiJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaAiGradingJobRepository extends JpaRepository<AiGradingJob, UUID> {

    @Query(value = """
            SELECT * FROM ai_grading_job
            WHERE (status = 'PENDING' AND next_run_at <= :now) OR (status = 'RUNNING' AND locked_at < :staleBefore)
            ORDER BY next_run_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<AiGradingJob> lockNext(@Param("now") Instant now, @Param("staleBefore") Instant staleBefore);

    List<AiGradingJob> findByStatusIn(List<AiJobStatus> statuses);
}
