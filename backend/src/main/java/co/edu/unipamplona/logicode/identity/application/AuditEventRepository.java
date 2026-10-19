package co.edu.unipamplona.logicode.identity.application;

import java.time.Instant;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.shared.application.PageResult;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    long countByActionAndSubjectAndOutcomeSince(AuditAction action, String subject, AuditEvent.Outcome outcome,
            Instant since);

    PageResult<AuditEvent> listLatest(int page, int size);
}
