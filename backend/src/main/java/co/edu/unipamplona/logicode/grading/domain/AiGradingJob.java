package co.edu.unipamplona.logicode.grading.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Trabajo de la cola de evaluación con IA (RNF-06 a RNF-08). Cada toma genera un {@code claimToken}: solo quien tiene
 * la toma vigente puede cerrar el trabajo, así un resultado nunca se aplica dos veces.
 */
@Entity
@Table(name = "ai_grading_job")
public class AiGradingJob {

    @Id
    private UUID id;

    @Column(name = "grade_id", nullable = false, updatable = false)
    private UUID gradeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiJobStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "requested_by", updatable = false)
    private UUID requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiGradingJob() {
    }

    /** @param requestedBy docente que pidió reintentar, o {@code null} si la entrega acaba de llegar */
    public AiGradingJob(UUID gradeId, UUID requestedBy, Instant now) {
        this.id = UUID.randomUUID();
        this.gradeId = gradeId;
        this.requestedBy = requestedBy;
        this.status = AiJobStatus.PENDING;
        this.nextRunAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Toma el trabajo (también uno en curso cuyo bloqueo venció) y devuelve la toma vigente. */
    public UUID claim(Instant now) {
        if (status != AiJobStatus.PENDING && status != AiJobStatus.RUNNING) {
            throw new IllegalStateException("El trabajo ya terminó");
        }
        this.status = AiJobStatus.RUNNING;
        this.attempts++;
        this.lockedAt = now;
        this.claimToken = UUID.randomUUID();
        this.updatedAt = now;
        return claimToken;
    }

    public boolean isClaimedBy(UUID token) {
        return status == AiJobStatus.RUNNING && token.equals(claimToken);
    }

    public void succeed(Instant now) {
        this.status = AiJobStatus.DONE;
        release(now);
    }

    /**
     * Registra un fallo técnico. Con intentos disponibles vuelve a la cola con espera creciente; si no, termina.
     *
     * @return {@code true} si se volverá a intentar
     */
    public boolean fail(String error, int maxAttempts, Duration retryDelay, Instant now) {
        this.lastError = error;
        release(now);
        if (attempts < maxAttempts) {
            this.status = AiJobStatus.PENDING;
            this.nextRunAt = now.plus(retryDelay.multipliedBy(attempts));
            return true;
        }
        this.status = AiJobStatus.FAILED;
        return false;
    }

    /** Cierra el trabajo sin evaluar (p. ej. la IA se desactivó con trabajos en cola). */
    public void cancel(String reason, Instant now) {
        this.lastError = reason;
        this.status = AiJobStatus.FAILED;
        release(now);
    }

    private void release(Instant now) {
        this.lockedAt = null;
        this.claimToken = null;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGradeId() {
        return gradeId;
    }

    public AiJobStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public String getLastError() {
        return lastError;
    }
}
