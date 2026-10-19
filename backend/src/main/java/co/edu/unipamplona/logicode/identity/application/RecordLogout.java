package co.edu.unipamplona.logicode.identity.application;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-03: registra el cierre de sesión en la auditoría. */
@Service
public class RecordLogout {

    private final AuditLog auditLog;

    public RecordLogout(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Transactional
    public void execute(Actor actor) {
        auditLog.record(AuditAction.LOGOUT, actor.userId(), actor.userId(), actor.cedula(), AuditEvent.Outcome.SUCCESS,
                null);
    }
}
