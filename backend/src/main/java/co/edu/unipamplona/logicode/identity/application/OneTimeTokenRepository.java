package co.edu.unipamplona.logicode.identity.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.OneTimeToken;
import co.edu.unipamplona.logicode.identity.domain.TokenPurpose;

public interface OneTimeTokenRepository {

    OneTimeToken save(OneTimeToken token);

    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    /** Invalida los tokens sin usar de una cuenta para un propósito (solo el último enlace sirve). */
    void invalidateUnused(UUID userId, TokenPurpose purpose, Instant now);
}
