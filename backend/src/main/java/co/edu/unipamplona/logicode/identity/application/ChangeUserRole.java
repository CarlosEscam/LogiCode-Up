package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.Permission;
import co.edu.unipamplona.logicode.identity.domain.Role;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-28, RN-04, RN-16: cambio de rol, reservado a las cuentas con privilegio completo. El privilegio completo no se
 * otorga por esta vía: una cuenta que pasa a Administrador queda como administrador ordinario.
 */
@Service
public class ChangeUserRole {

    private final UserAccountRepository accounts;
    private final SessionRevoker sessionRevoker;
    private final AuditLog auditLog;
    private final Clock clock;

    public ChangeUserRole(UserAccountRepository accounts, SessionRevoker sessionRevoker, AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
        this.sessionRevoker = sessionRevoker;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public UserAccount execute(Actor actor, UUID userId, Role newRole) {
        if (!actor.can(Permission.CHANGE_ROLES)) {
            auditLog.record(AuditAction.ROLE_CHANGED, actor.userId(), userId, null, AuditEvent.Outcome.DENIED, null);
            throw ApplicationException.forbidden("Solo las cuentas iniciales con privilegios completos pueden cambiar roles.");
        }
        if (newRole == null) {
            throw ApplicationException.businessRule("Indica el rol nuevo.");
        }
        UserAccount account = accounts.findById(userId)
                .orElseThrow(() -> ApplicationException.notFound("La cuenta no existe."));
        Role previous = account.getRole();
        if (previous == newRole) {
            return account;
        }
        if (newRole != Role.ADMIN) {
            FullPrivilegeGuard.ensureAnotherRemains(accounts, account);
        }
        account.changeRole(newRole, clock.instant());
        accounts.save(account);
        sessionRevoker.revokeAll(account.getId());
        auditLog.record(AuditAction.ROLE_CHANGED, actor.userId(), account.getId(), account.getCedula(),
                AuditEvent.Outcome.SUCCESS, previous + " -> " + newRole);
        return account;
    }
}
