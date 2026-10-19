package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activar o desactivar cuentas (gestión ordinaria de usuarios). Una cuenta con privilegio completo solo la puede
 * desactivar otra cuenta con privilegio completo, y nunca la última activa (RN-16). Desactivar cierra sus sesiones.
 */
@Service
public class ChangeUserStatus {

    private final UserAccountRepository accounts;
    private final SessionRevoker sessionRevoker;
    private final AuditLog auditLog;
    private final Clock clock;

    public ChangeUserStatus(UserAccountRepository accounts, SessionRevoker sessionRevoker, AuditLog auditLog,
            Clock clock) {
        this.accounts = accounts;
        this.sessionRevoker = sessionRevoker;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public UserAccount execute(Actor actor, UUID userId, AccountStatus newStatus) {
        actor.require(Permission.MANAGE_USERS);
        if (newStatus != AccountStatus.ACTIVE && newStatus != AccountStatus.DISABLED) {
            throw ApplicationException.businessRule("El estado debe ser ACTIVE o DISABLED.");
        }
        UserAccount account = accounts.findById(userId)
                .orElseThrow(() -> ApplicationException.notFound("La cuenta no existe."));
        if (account.isFullPrivileges() && !actor.fullPrivileges()) {
            throw ApplicationException.forbidden("Solo una cuenta con privilegios completos puede cambiar el estado de otra igual.");
        }
        if (account.getStatus() == AccountStatus.PENDING_APPROVAL && newStatus == AccountStatus.ACTIVE) {
            throw ApplicationException.conflict("Las solicitudes docentes se aprueban con la opción Aprobar.");
        }
        if (account.getStatus() == newStatus) {
            return account;
        }
        if (newStatus == AccountStatus.DISABLED) {
            FullPrivilegeGuard.ensureAnotherRemains(accounts, account);
        }
        AccountStatus previous = account.getStatus();
        account.changeStatus(newStatus, clock.instant());
        accounts.save(account);
        if (newStatus == AccountStatus.DISABLED) {
            sessionRevoker.revokeAll(account.getId());
        }
        auditLog.record(AuditAction.STATUS_CHANGED, actor.userId(), account.getId(), account.getCedula(),
                AuditEvent.Outcome.SUCCESS, previous + " -> " + newStatus);
        return account;
    }
}
