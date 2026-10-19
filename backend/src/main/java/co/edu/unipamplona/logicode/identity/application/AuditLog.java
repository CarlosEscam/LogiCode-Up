package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import org.springframework.stereotype.Component;

/** Registra eventos de auditoría de acceso y administración (Etapa 3: auditoría de acceso). */
@Component
public class AuditLog {

    private final AuditEventRepository repository;
    private final RequestContext requestContext;
    private final Clock clock;

    public AuditLog(AuditEventRepository repository, RequestContext requestContext, Clock clock) {
        this.repository = repository;
        this.requestContext = requestContext;
        this.clock = clock;
    }

    public void record(AuditAction action, UUID actorId, UUID targetUserId, String subject,
            AuditEvent.Outcome outcome, String details) {
        repository.save(new AuditEvent(action, actorId, targetUserId, subject, outcome,
                requestContext.requestId(), details, clock.instant()));
    }
}
