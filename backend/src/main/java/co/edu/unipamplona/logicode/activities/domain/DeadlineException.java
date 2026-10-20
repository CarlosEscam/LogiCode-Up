package co.edu.unipamplona.logicode.activities.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * RN-05: ampliación de plazo o intentos adicionales para un estudiante en una actividad. Permite entregar después
 * del cierre ("reabrir") o volver a entregar (segundo intento).
 */
@Entity
@Table(name = "deadline_exception")
public class DeadlineException {

    @Id
    private UUID id;

    @Column(name = "activity_id", nullable = false, updatable = false)
    private UUID activityId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "extended_until")
    private Instant extendedUntil;

    @Column(name = "extra_attempts", nullable = false)
    private int extraAttempts;

    private String reason;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Version
    private long version;

    protected DeadlineException() {
    }

    public DeadlineException(UUID activityId, UUID studentId) {
        this.id = UUID.randomUUID();
        this.activityId = activityId;
        this.studentId = studentId;
    }

    public void grant(Instant extendedUntil, int extraAttempts, String reason, UUID grantedBy, Instant now) {
        if (extendedUntil == null && extraAttempts <= 0) {
            throw new IllegalArgumentException("La excepción debe ampliar el plazo o dar intentos");
        }
        this.extendedUntil = extendedUntil;
        this.extraAttempts = extraAttempts;
        this.reason = reason;
        this.grantedBy = grantedBy;
        this.grantedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public Instant getExtendedUntil() {
        return extendedUntil;
    }

    public int getExtraAttempts() {
        return extraAttempts;
    }

    public String getReason() {
        return reason;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }
}
