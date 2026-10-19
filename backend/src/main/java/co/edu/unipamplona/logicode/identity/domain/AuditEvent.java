package co.edu.unipamplona.logicode.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Evento de auditoría de acceso y administración. Nunca guarda contraseñas ni tokens. */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    public enum Outcome {
        SUCCESS,
        FAILURE,
        DENIED,
        LIMIT_REACHED
    }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditAction action;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "target_user_id", updatable = false)
    private UUID targetUserId;

    /** Identificador ingresado (cédula o correo) cuando no hay cuenta asociada o para limitar intentos. */
    @Column(updatable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Outcome outcome;

    @Column(name = "request_id", updatable = false)
    private String requestId;

    @Column(updatable = false)
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(AuditAction action, UUID actorId, UUID targetUserId, String subject, Outcome outcome,
            String requestId, String details, Instant now) {
        this.id = UUID.randomUUID();
        this.action = action;
        this.actorId = actorId;
        this.targetUserId = targetUserId;
        this.subject = subject;
        this.outcome = outcome;
        this.requestId = requestId;
        this.details = details;
        this.occurredAt = now;
    }

    public UUID getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public UUID getActorId() {
        return actorId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public String getSubject() {
        return subject;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
