package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.RecoveryRequestRepository;
import co.edu.unipamplona.logicode.identity.domain.RecoveryKind;
import co.edu.unipamplona.logicode.identity.domain.RecoveryRequest;
import org.springframework.stereotype.Repository;

@Repository
class RecoveryRequestRepositoryAdapter implements RecoveryRequestRepository {

    private final JpaRecoveryRequestRepository jpa;

    RecoveryRequestRepositoryAdapter(JpaRecoveryRequestRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RecoveryRequest save(RecoveryRequest request) {
        return jpa.save(request);
    }

    @Override
    public long countCountedFor(UUID userId, RecoveryKind kind, LocalDate businessDate) {
        return jpa.countCounted(userId, kind, businessDate);
    }
}
