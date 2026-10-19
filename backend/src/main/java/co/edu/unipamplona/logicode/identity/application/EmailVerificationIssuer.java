package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.OneTimeToken;
import co.edu.unipamplona.logicode.identity.domain.TokenPurpose;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import org.springframework.stereotype.Component;

/**
 * Emite un enlace de verificación de correo de un solo uso y lo envía con Brevo (RF-29, RN-24).
 * Solo el último enlace emitido queda vigente.
 */
@Component
public class EmailVerificationIssuer {

    private final OneTimeTokenRepository tokens;
    private final EmailSender emailSender;
    private final EmailTemplates templates;
    private final AuditLog auditLog;
    private final Clock clock;

    public EmailVerificationIssuer(OneTimeTokenRepository tokens, EmailSender emailSender, EmailTemplates templates,
            AuditLog auditLog, Clock clock) {
        this.tokens = tokens;
        this.emailSender = emailSender;
        this.templates = templates;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public void issue(UserAccount account, String targetEmail) {
        var now = clock.instant();
        tokens.invalidateUnused(account.getId(), TokenPurpose.EMAIL_VERIFICATION, now);
        String token = SecureTokens.newToken();
        tokens.save(OneTimeToken.emailVerification(account.getId(), SecureTokens.hash(token), targetEmail, now));
        emailSender.send(templates.emailVerification(targetEmail, account.getFullName(), token));
        auditLog.record(AuditAction.EMAIL_VERIFICATION_SENT, account.getId(), account.getId(), null,
                AuditEvent.Outcome.SUCCESS, null);
    }
}
