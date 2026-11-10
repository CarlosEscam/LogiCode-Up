package co.edu.unipamplona.logicode.grading.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Calibración de la IA en un curso (T-90). Es opcional: un curso sin registro no está en calibración. Mientras está
 * abierta, la IA no publica notas sola aunque su confianza sea de 90 % o más: el docente confirma cada propuesta y así
 * se mide cuánto coincide la IA con él. Se cierra cuando hay suficientes comparaciones; desde entonces vuelve a aplicar
 * la regla del 90 % (RN-09, RN-11).
 */
@Entity
@Table(name = "ai_calibration")
public class AiCalibration {

    @Id
    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected AiCalibration() {
    }

    /** El docente enciende la calibración del curso. */
    public static AiCalibration open(UUID courseId, Instant now) {
        AiCalibration calibration = new AiCalibration();
        calibration.courseId = courseId;
        calibration.updatedAt = now;
        return calibration;
    }

    public boolean isOpen() {
        return closedAt == null;
    }

    /** @throws IllegalArgumentException si aún no hay {@code minimum} comparaciones */
    public void close(UUID by, CalibrationStats stats, int minimum, Instant now) {
        if (!isOpen()) {
            throw new IllegalStateException("La calibración ya está cerrada");
        }
        if (stats.compared() < minimum) {
            throw new IllegalArgumentException("Para cerrar la calibración hacen falta al menos " + minimum
                    + " notas comparadas; van " + stats.compared() + ".");
        }
        this.closedAt = now;
        this.closedBy = by;
        this.updatedAt = now;
    }

    public void reopen(Instant now) {
        if (isOpen()) {
            throw new IllegalStateException("La calibración ya está abierta");
        }
        this.closedAt = null;
        this.closedBy = null;
        this.updatedAt = now;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
