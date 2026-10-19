package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.time.Instant;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.OneTimeToken;
import co.edu.unipamplona.logicode.identity.domain.TokenPurpose;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-29, RN-24, RN-25: confirma la titularidad de un correo con un enlace de un solo uso. Si el enlace corresponde
 * a un correo nuevo, este reemplaza al anterior solo en este momento.
 */
@Service
public class ConfirmEmail {

    public record Result(String email, boolean passwordSetupRequired) {
    }

    static final String INVALID_LINK = "El enlace no es válido, ya se usó o venció. Solicita uno nuevo.";

    private final OneTimeTokenRepository tokens;
    private final UserAccountRepository accounts;
    private final AuditLog auditLog;
    private final Clock clock;

    public ConfirmEmail(OneTimeTokenRepository tokens, UserAccountRepository accounts, AuditLog auditLog, Clock clock) {
        this.tokens = tokens;
        this.accounts = accounts;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public Result execute(String rawToken) {
        Instant now = clock.instant();
        OneTimeToken token = TokenLookup.usable(tokens, rawToken, TokenPurpose.EMAIL_VERIFICATION, now)
                .orElseThrow(() -> ApplicationException.businessRule(INVALID_LINK));
        UserAccount account = accounts.findById(token.getUserId())
                .orElseThrow(() -> ApplicationException.businessRule(INVALID_LINK));

        String target = token.getTargetEmail();
        if (target.equalsIgnoreCase(account.getEmail())) {
            account.markEmailVerified(now);
        } else {
            accounts.findByEmailIgnoreCase(target)
                    .filter(other -> !other.getId().equals(account.getId()))
                    .ifPresent(other -> {
                        throw ApplicationException.conflict("Este correo ya está registrado en otra cuenta.");
                    });
            account.replaceEmailWithVerified(target, now);
        }
        token.consume(now);
        tokens.save(token);
        accounts.save(account);
        auditLog.record(AuditAction.EMAIL_VERIFIED, account.getId(), account.getId(), null, AuditEvent.Outcome.SUCCESS,
                null);
        return new Result(account.getEmail(), !account.hasPassword());
    }
}
