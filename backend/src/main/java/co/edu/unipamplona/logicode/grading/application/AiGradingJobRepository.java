package co.edu.unipamplona.logicode.grading.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiGradingJob;

public interface AiGradingJobRepository {

    AiGradingJob save(AiGradingJob job);

    Optional<AiGradingJob> findById(UUID id);

    /**
     * Bloquea el siguiente trabajo listo (pendiente y vencido, o en curso con bloqueo anterior a {@code staleBefore})
     * saltando los que otro proceso tiene bloqueados.
     */
    Optional<AiGradingJob> lockNext(Instant now, Instant staleBefore);

    /** Trabajos pendientes o en curso. */
    List<AiGradingJob> findActive();
}
