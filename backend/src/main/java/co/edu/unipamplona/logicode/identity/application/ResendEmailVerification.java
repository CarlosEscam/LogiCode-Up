package co.edu.unipamplona.logicode.identity.application;

import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reenvía el enlace de verificación del correo actual de la sesión. */
@Service
public class ResendEmailVerification {

    private final UserAccountRepository accounts;
    private final EmailVerificationIssuer issuer;

    public ResendEmailVerification(UserAccountRepository accounts, EmailVerificationIssuer issuer) {
        this.accounts = accounts;
        this.issuer = issuer;
    }

    @Transactional
    public void execute(Actor actor) {
        UserAccount account = accounts.findById(actor.userId())
                .orElseThrow(() -> ApplicationException.unauthenticated("Debes iniciar sesión."));
        if (account.isEmailVerified()) {
            throw ApplicationException.businessRule("Tu correo ya está verificado.");
        }
        issuer.issue(account, account.getEmail());
    }
}
