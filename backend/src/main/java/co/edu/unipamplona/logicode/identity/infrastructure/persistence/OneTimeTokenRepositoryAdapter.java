package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.OneTimeTokenRepository;
import co.edu.unipamplona.logicode.identity.domain.OneTimeToken;
import co.edu.unipamplona.logicode.identity.domain.TokenPurpose;
import org.springframework.stereotype.Repository;

@Repository
class OneTimeTokenRepositoryAdapter implements OneTimeTokenRepository {

    private final JpaOneTimeTokenRepository jpa;

    OneTimeTokenRepositoryAdapter(JpaOneTimeTokenRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OneTimeToken save(OneTimeToken token) {
        return jpa.save(token);
    }

    @Override
    public Optional<OneTimeToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash);
    }

    @Override
    public void invalidateUnused(UUID userId, TokenPurpose purpose, Instant now) {
        jpa.invalidateUnused(userId, purpose, now);
    }
}
