package co.edu.unipamplona.logicode.identity.application;

import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga la cuenta de la sesión en cada solicitud, para que un cambio de rol o una desactivación tenga efecto
 * inmediato. Solo las cuentas activas producen un actor.
 */
@Service
public class GetCurrentActor {

    private final UserAccountRepository accounts;

    public GetCurrentActor(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Optional<Actor> byUserId(UUID userId) {
        return accounts.findById(userId)
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .map(Actor::of);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> account(UUID userId) {
        return accounts.findById(userId);
    }
}
