package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registro inmutable de una nota publicada o corregida (RF-17, RN-09). */
@Entity
@Table(name = "grade_change")
public class GradeChange {

    @Id
    private UUID id;

    @Column(name = "grade_id", nullable = false, updatable = false)
    private UUID gradeId;

    @Column(name = "previous_score", updatable = false)
    private BigDecimal previousScore;

    @Column(name = "new_score", nullable = false, updatable = false)
    private BigDecimal newScore;

    @Column(updatable = false)
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private GradeSource source;

    @Column(name = "changed_by", updatable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected GradeChange() {
    }

    GradeChange(UUID gradeId, BigDecimal previousScore, BigDecimal newScore, String feedback, GradeSource source,
            UUID changedBy, Instant changedAt) {
        this.id = UUID.randomUUID();
        this.gradeId = gradeId;
        this.previousScore = previousScore;
        this.newScore = newScore;
        this.feedback = feedback;
        this.source = source;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGradeId() {
        return gradeId;
    }

    public BigDecimal getPreviousScore() {
        return previousScore;
    }

    public BigDecimal getNewScore() {
        return newScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public GradeSource getSource() {
        return source;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
