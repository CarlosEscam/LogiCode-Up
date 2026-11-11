package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.OneTimeToken;
import co.edu.unipamplona.logicode.identity.domain.TokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface JpaOneTimeTokenRepository extends JpaRepository<OneTimeToken, UUID> {

    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("update OneTimeToken t set t.usedAt = :now where t.userId = :userId and t.purpose = :purpose and t.usedAt is null")
    int invalidateUnused(UUID userId, TokenPurpose purpose, Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("""
            update OneTimeToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null
              and t.purpose = co.edu.unipamplona.logicode.identity.domain.TokenPurpose.EMAIL_VERIFICATION
              and lower(t.targetEmail) <> lower(:currentEmail)""")
    int invalidatePendingEmailChanges(UUID userId, String currentEmail, Instant now);
}
