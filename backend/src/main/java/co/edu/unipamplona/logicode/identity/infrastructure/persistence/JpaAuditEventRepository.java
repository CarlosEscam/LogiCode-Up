package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaAuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    long countByActionAndSubjectAndOutcomeAndOccurredAtAfter(AuditAction action, String subject,
            AuditEvent.Outcome outcome, Instant since);

    long countByActionAndSubjectAndOutcomeAndDetailsAndOccurredAtAfter(AuditAction action, String subject,
            AuditEvent.Outcome outcome, String details, Instant since);

    @org.springframework.data.jpa.repository.Query(nativeQuery = true,
            value = "select count(*) from (select pg_advisory_xact_lock(hashtext(:subject))) bloqueo")
    long lockSubject(String subject);
}
