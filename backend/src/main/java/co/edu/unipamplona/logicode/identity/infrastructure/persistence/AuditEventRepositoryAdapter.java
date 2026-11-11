package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.time.Instant;

import co.edu.unipamplona.logicode.identity.application.AuditEventRepository;
import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class AuditEventRepositoryAdapter implements AuditEventRepository {

    private final JpaAuditEventRepository jpa;

    AuditEventRepositoryAdapter(JpaAuditEventRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        return jpa.save(event);
    }

    @Override
    public long countByActionAndSubjectAndOutcomeSince(AuditAction action, String subject, AuditEvent.Outcome outcome,
            Instant since) {
        return jpa.countByActionAndSubjectAndOutcomeAndOccurredAtAfter(action, subject, outcome, since);
    }

    @Override
    public long countByActionAndSubjectAndOutcomeAndDetailsSince(AuditAction action, String subject,
            AuditEvent.Outcome outcome, String details, Instant since) {
        return jpa.countByActionAndSubjectAndOutcomeAndDetailsAndOccurredAtAfter(action, subject, outcome, details,
                since);
    }

    @Override
    public void lockSubject(String subject) {
        jpa.lockSubject(subject);
    }

    @Override
    public PageResult<AuditEvent> listLatest(int page, int size) {
        return PageResults.of(jpa.findAll(PageRequest.of(page, size, Sort.by("occurredAt").descending())));
    }
}
