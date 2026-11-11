package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.time.Instant;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.OneTimeToken;
import co.edu.unipamplona.logicode.identity.domain.TokenPurpose;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-04, RN-23: guarda la contraseña nueva con un token de recuperación válido, consume el token, rota los hashes
 * y cierra todas las sesiones activas de la cuenta.
 */
@Service
public class ResetPassword {

    private final OneTimeTokenRepository tokens;
    private final UserAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final SessionRevoker sessionRevoker;
    private final AuditLog auditLog;
    private final Clock clock;

    public ResetPassword(OneTimeTokenRepository tokens, UserAccountRepository accounts, PasswordHasher passwordHasher,
            SessionRevoker sessionRevoker, AuditLog auditLog, Clock clock) {
        this.tokens = tokens;
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.sessionRevoker = sessionRevoker;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public void execute(String rawToken, String newPassword) {
        Instant now = clock.instant();
        OneTimeToken token = TokenLookup.usable(tokens, rawToken, TokenPurpose.PASSWORD_RESET, now)
                .orElseThrow(() -> ApplicationException.businessRule(ConfirmEmail.INVALID_LINK));
        RegistrationRules.requireValidPassword(newPassword);
        UserAccount account = accounts.findById(token.getUserId())
                .filter(a -> a.getStatus() != AccountStatus.DISABLED)
                .orElseThrow(() -> ApplicationException.businessRule(ConfirmEmail.INVALID_LINK));

        account.changePassword(passwordHasher.hash(newPassword), now);
        token.consume(now);
        tokens.save(token);
        tokens.invalidateUnused(account.getId(), TokenPurpose.PASSWORD_RESET, now);
        tokens.invalidatePendingEmailChanges(account.getId(), account.getEmail(), now); // P-02: cancela un cambio pendiente
        accounts.save(account);
        sessionRevoker.revokeAll(account.getId());
        auditLog.record(AuditAction.PASSWORD_RESET, account.getId(), account.getId(), null, AuditEvent.Outcome.SUCCESS,
                "sesiones cerradas");
    }
}
