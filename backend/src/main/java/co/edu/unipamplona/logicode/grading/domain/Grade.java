package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Nota de una entrega (RN-06: 0,0 a 5,0 con un decimal). Nace en la bandeja Por revisar con la versión de rúbrica
 * vigente; al calificar se publica, y cada corrección queda en el historial (RF-17).
 */
@Entity
@Table(name = "grade")
public class Grade {

    public static final BigDecimal MAX_SCORE = new BigDecimal("5.0");

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradeStatus status;

    private BigDecimal score;

    private String feedback;

    @Enumerated(EnumType.STRING)
    private GradeSource source;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Grade() {
    }

    public Grade(UUID submissionId, UUID rubricVersionId, Instant now) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.rubricVersionId = rubricVersionId;
        this.status = GradeStatus.PENDING_REVIEW;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static boolean isValidScore(BigDecimal score) {
        return score != null && score.signum() >= 0 && score.compareTo(MAX_SCORE) <= 0
                && score.stripTrailingZeros().scale() <= 1;
    }

    /** Publica o corrige la nota y devuelve el registro para el historial. */
    public GradeChange publish(BigDecimal newScore, String newFeedback, GradeSource by, UUID gradedBy, Instant now) {
        if (!isValidScore(newScore)) {
            throw new IllegalArgumentException("Nota fuera de la escala 0,0 a 5,0");
        }
        BigDecimal normalized = newScore.setScale(1, RoundingMode.UNNECESSARY);
        GradeChange change = new GradeChange(id, score, normalized, newFeedback, by, gradedBy, now);
        this.score = normalized;
        this.feedback = newFeedback;
        this.source = by;
        this.gradedBy = gradedBy;
        this.gradedAt = now;
        this.status = GradeStatus.PUBLISHED;
        this.updatedAt = now;
        return change;
    }

    public boolean isPublished() {
        return status == GradeStatus.PUBLISHED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public UUID getRubricVersionId() {
        return rubricVersionId;
    }

    public GradeStatus getStatus() {
        return status;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }

    public GradeSource getSource() {
        return source;
    }

    public UUID getGradedBy() {
        return gradedBy;
    }

    public Instant getGradedAt() {
        return gradedAt;
    }
}
