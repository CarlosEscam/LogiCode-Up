package co.edu.unipamplona.logicode.grading.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.application.AiGradingJobRepository;
import co.edu.unipamplona.logicode.grading.domain.AiGradingJob;
import co.edu.unipamplona.logicode.grading.domain.AiJobStatus;
import org.springframework.stereotype.Repository;

@Repository
class AiGradingJobRepositoryAdapter implements AiGradingJobRepository {

    private final JpaAiGradingJobRepository jpa;

    AiGradingJobRepositoryAdapter(JpaAiGradingJobRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AiGradingJob save(AiGradingJob job) {
        return jpa.save(job);
    }

    @Override
    public Optional<AiGradingJob> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<AiGradingJob> lockNext(Instant now, Instant staleBefore) {
        return jpa.lockNext(now, staleBefore);
    }

    @Override
    public List<AiGradingJob> findActive() {
        return jpa.findByStatusIn(List.of(AiJobStatus.PENDING, AiJobStatus.RUNNING));
    }
}
