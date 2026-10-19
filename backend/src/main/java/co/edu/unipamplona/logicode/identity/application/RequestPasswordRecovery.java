package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.EmailAddress;
import co.edu.unipamplona.logicode.identity.domain.OneTimeToken;
import co.edu.unipamplona.logicode.identity.domain.RecoveryKind;
import co.edu.unipamplona.logicode.identity.domain.RecoveryRequest;
import co.edu.unipamplona.logicode.identity.domain.TokenPurpose;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-04, RN-23: solicitud de restablecimiento de contraseña con un correo verificado.
 * <p>
 * Siempre termina sin error para que la respuesta sea la misma exista o no la cuenta. Solo se envía el enlace si
 * el correo pertenece a una cuenta no desactivada con correo verificado y no se superó el límite de
 * {@value RecoveryRequest#DAILY_LIMIT} solicitudes en el día calendario de Colombia.
 */
@Service
public class RequestPasswordRecovery {

    private final UserAccountRepository accounts;
    private final OneTimeTokenRepository tokens;
    private final RecoveryRequestRepository recoveryRequests;
    private final EmailSender emailSender;
    private final EmailTemplates templates;
    private final AuditLog auditLog;
    private final Clock clock;

    public RequestPasswordRecovery(UserAccountRepository accounts, OneTimeTokenRepository tokens,
            RecoveryRequestRepository recoveryRequests, EmailSender emailSender, EmailTemplates templates,
            AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.recoveryRequests = recoveryRequests;
        this.emailSender = emailSender;
        this.templates = templates;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public void execute(String rawEmail) {
        String email = EmailAddress.normalize(rawEmail);
        Optional<UserAccount> found = email == null ? Optional.empty()
                : accounts.findByEmailIgnoreCase(email)
                        .filter(UserAccount::isEmailVerified)
                        .filter(a -> a.getStatus() != AccountStatus.DISABLED);
        if (found.isEmpty()) {
            auditLog.record(AuditAction.PASSWORD_RECOVERY_REQUESTED, null, null, email, AuditEvent.Outcome.FAILURE,
                    "sin cuenta con ese correo verificado");
            return;
        }

        UserAccount account = found.get();
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        long counted = recoveryRequests.countCountedFor(account.getId(), RecoveryKind.PASSWORD_RESET, today);
        if (!RecoveryRequest.withinDailyLimit(counted)) {
            recoveryRequests.save(new RecoveryRequest(account.getId(), RecoveryKind.PASSWORD_RESET, today,
                    RecoveryRequest.Outcome.LIMIT_REACHED, now));
            auditLog.record(AuditAction.PASSWORD_RECOVERY_REQUESTED, null, account.getId(), email,
                    AuditEvent.Outcome.LIMIT_REACHED, null);
            return;
        }

        tokens.invalidateUnused(account.getId(), TokenPurpose.PASSWORD_RESET, now);
        String token = SecureTokens.newToken();
        tokens.save(OneTimeToken.passwordReset(account.getId(), SecureTokens.hash(token), now));
        recoveryRequests.save(new RecoveryRequest(account.getId(), RecoveryKind.PASSWORD_RESET, today,
                RecoveryRequest.Outcome.ACCEPTED, now));
        emailSender.send(templates.passwordReset(account.getEmail(), account.getFullName(), token));
        auditLog.record(AuditAction.PASSWORD_RECOVERY_REQUESTED, null, account.getId(), email,
                AuditEvent.Outcome.SUCCESS, null);
    }
}
