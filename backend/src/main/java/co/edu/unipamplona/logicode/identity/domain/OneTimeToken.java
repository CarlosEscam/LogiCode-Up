package co.edu.unipamplona.logicode.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Token de un solo uso (RN-23, RN-24). Solo se guarda su hash; queda invalidado al usarse o al vencer.
 */
@Entity
@Table(name = "one_time_token")
public class OneTimeToken {

    /** Vigencia del enlace de recuperación de contraseña (RN-23). */
    public static final Duration PASSWORD_RESET_VALIDITY = Duration.ofMinutes(60);

    /** Vigencia del enlace de verificación de correo (decisión técnica T-13). */
    public static final Duration EMAIL_VERIFICATION_VALIDITY = Duration.ofHours(24);

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    /** Correo al que se envió el enlace de verificación; puede ser un correo nuevo pendiente (RN-25). */
    @Column(name = "target_email", updatable = false)
    private String targetEmail;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OneTimeToken() {
    }

    private OneTimeToken(UUID userId, TokenPurpose purpose, String tokenHash, String targetEmail, Instant now,
            Duration validity) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId);
        this.purpose = Objects.requireNonNull(purpose);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.targetEmail = targetEmail;
        this.createdAt = now;
        this.expiresAt = now.plus(validity);
    }

    public static OneTimeToken passwordReset(UUID userId, String tokenHash, Instant now) {
        return new OneTimeToken(userId, TokenPurpose.PASSWORD_RESET, tokenHash, null, now, PASSWORD_RESET_VALIDITY);
    }

    public static OneTimeToken emailVerification(UUID userId, String tokenHash, String targetEmail, Instant now) {
        return new OneTimeToken(userId, TokenPurpose.EMAIL_VERIFICATION, tokenHash,
                Objects.requireNonNull(targetEmail), now, EMAIL_VERIFICATION_VALIDITY);
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void consume(Instant now) {
        if (!isUsable(now)) {
            throw new IllegalStateException("El token ya se usó o venció");
        }
        this.usedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public TokenPurpose getPurpose() {
        return purpose;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
