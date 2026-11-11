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

    /**
     * Cambio asistido de correo (RF-30, P-02): enlace al correo nuevo que sirve después de la espera y aviso al correo
     * actual, para que el dueño pueda cancelarlo cambiando su contraseña si no lo pidió.
     */
    public void issueEmailChange(UserAccount account, String newEmail) {
        var now = clock.instant();
        tokens.invalidateUnused(account.getId(), TokenPurpose.EMAIL_VERIFICATION, now);
        String token = SecureTokens.newToken();
        OneTimeToken saved = tokens.save(
                OneTimeToken.emailChange(account.getId(), SecureTokens.hash(token), newEmail, now));
        String from = templates.dateTime(saved.usableFrom());
        emailSender.send(templates.emailChange(newEmail, account.getFullName(), token, from));
        emailSender.send(templates.emailChangeNotice(account.getEmail(), account.getFullName(), newEmail, from));
        auditLog.record(AuditAction.EMAIL_VERIFICATION_SENT, account.getId(), account.getId(), null,
                AuditEvent.Outcome.SUCCESS, "cambio de correo: aviso al correo actual");
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
