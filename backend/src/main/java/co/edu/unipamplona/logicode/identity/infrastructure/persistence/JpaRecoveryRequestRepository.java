package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.RecoveryKind;
import co.edu.unipamplona.logicode.identity.domain.RecoveryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaRecoveryRequestRepository extends JpaRepository<RecoveryRequest, UUID> {

    @Query("""
            select count(r) from RecoveryRequest r
            where r.userId = :userId and r.kind = :kind and r.businessDate = :day
              and r.outcome <> co.edu.unipamplona.logicode.identity.domain.RecoveryRequest.Outcome.LIMIT_REACHED
            """)
    long countCounted(UUID userId, RecoveryKind kind, LocalDate day);
}
