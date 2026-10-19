package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.Permission;
import co.edu.unipamplona.logicode.identity.domain.Role;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-02, RN-02: un administrador aprueba una solicitud de cuenta docente. */
@Service
public class ApproveTeacher {

    private final UserAccountRepository accounts;
    private final AuditLog auditLog;
    private final Clock clock;

    public ApproveTeacher(UserAccountRepository accounts, AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public UserAccount execute(Actor actor, UUID userId) {
        actor.require(Permission.APPROVE_TEACHERS);
        UserAccount account = accounts.findById(userId)
                .orElseThrow(() -> ApplicationException.notFound("La cuenta no existe."));
        if (account.getRole() != Role.TEACHER || account.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw ApplicationException.conflict("La cuenta no es una solicitud docente pendiente.");
        }
        account.approveAsTeacher(clock.instant());
        accounts.save(account);
        auditLog.record(AuditAction.TEACHER_APPROVED, actor.userId(), account.getId(), account.getCedula(),
                AuditEvent.Outcome.SUCCESS, null);
        return account;
    }
}
