package co.edu.unipamplona.logicode.identity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de una solicitud de recuperación. Las solicitudes aceptadas cuentan para el límite de
 * {@value #DAILY_LIMIT} por cuenta, por flujo y por día calendario en Colombia (RN-23, RN-25, D-04).
 */
@Entity
@Table(name = "recovery_request")
public class RecoveryRequest {

    public static final int DAILY_LIMIT = 2;

    public enum Outcome {
        /** Se envió el correo; cuenta para el límite. */
        ACCEPTED,
        /** Los datos no coincidieron (cambio asistido de correo); cuenta para el límite. */
        REJECTED,
        /** Se alcanzó el límite diario; no se envió nada y no cuenta. */
        LIMIT_REACHED
    }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private RecoveryKind kind;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Outcome outcome;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    protected RecoveryRequest() {
    }

    public RecoveryRequest(UUID userId, RecoveryKind kind, LocalDate businessDate, Outcome outcome, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.kind = kind;
        this.businessDate = businessDate;
        this.outcome = outcome;
        this.requestedAt = now;
    }

    /** Indica si una cuenta todavía puede hacer otra solicitud hoy, dado cuántas cuentan ya para el límite. */
    public static boolean withinDailyLimit(long countedToday) {
        return countedToday < DAILY_LIMIT;
    }
}
