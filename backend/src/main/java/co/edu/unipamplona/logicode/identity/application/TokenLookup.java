package co.edu.unipamplona.logicode.identity.application;

import java.time.Instant;
import java.util.Optional;

import co.edu.unipamplona.logicode.identity.domain.OneTimeToken;
import co.edu.unipamplona.logicode.identity.domain.TokenPurpose;

final class TokenLookup {

    private TokenLookup() {
    }

    static Optional<OneTimeToken> usable(OneTimeTokenRepository tokens, String rawToken, TokenPurpose purpose,
            Instant now) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 100) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(SecureTokens.hash(rawToken.trim()))
                .filter(t -> t.getPurpose() == purpose)
                .filter(t -> t.isUsable(now));
    }
}
