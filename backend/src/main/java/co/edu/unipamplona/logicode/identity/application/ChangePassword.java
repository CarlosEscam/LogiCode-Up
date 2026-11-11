package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambio de contraseña con sesión iniciada. Rota los hashes y cierra todas las sesiones, incluida la actual.
 */
@Service
public class ChangePassword {

    private final UserAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final SessionRevoker sessionRevoker;
    private final OneTimeTokenRepository tokens;
    private final AuditLog auditLog;
    private final Clock clock;

    public ChangePassword(UserAccountRepository accounts, PasswordHasher passwordHasher, SessionRevoker sessionRevoker,
            OneTimeTokenRepository tokens, AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.passwordHasher = passwordHasher;
        this.sessionRevoker = sessionRevoker;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public void execute(Actor actor, String currentPassword, String newPassword) {
        UserAccount account = accounts.findById(actor.userId())
                .orElseThrow(() -> ApplicationException.unauthenticated("Debes iniciar sesión."));
        if (currentPassword == null || !account.hasPassword()
                || !passwordHasher.matches(currentPassword, account.getPasswordHash())) {
            auditLog.record(AuditAction.PASSWORD_CHANGED, actor.userId(), actor.userId(), null,
                    AuditEvent.Outcome.FAILURE, "contraseña actual incorrecta");
            throw ApplicationException.businessRule("La contraseña actual no es correcta.");
        }
        RegistrationRules.requireValidPassword(newPassword);
        account.changePassword(passwordHasher.hash(newPassword), clock.instant());
        accounts.save(account);
        // P-02: cambiar la contraseña cancela un cambio de correo pendiente (lo dice el aviso al correo actual).
        tokens.invalidatePendingEmailChanges(account.getId(), account.getEmail(), clock.instant());
        sessionRevoker.revokeAll(account.getId());
        auditLog.record(AuditAction.PASSWORD_CHANGED, actor.userId(), actor.userId(), null, AuditEvent.Outcome.SUCCESS,
                "sesiones cerradas");
    }
}
