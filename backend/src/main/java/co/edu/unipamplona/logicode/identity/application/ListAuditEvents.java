package co.edu.unipamplona.logicode.identity.application;

import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de la auditoría de acceso, del más reciente al más antiguo. */
@Service
public class ListAuditEvents {

    private final AuditEventRepository auditEvents;

    public ListAuditEvents(AuditEventRepository auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public PageResult<AuditEvent> execute(Actor actor, int page, int size) {
        actor.require(Permission.VIEW_AUDIT);
        return auditEvents.listLatest(Math.max(page, 0), Math.clamp(size, 1, 100));
    }
}
