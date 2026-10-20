package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Registro inmutable de una evaluación con IA (RF-17, RNF-06): lo que el modelo leyó, legibilidad, confianza
 * reportada, puntos por criterio y la nota propuesta. Un fallo técnico queda con su error y sin nota.
 */
@Entity
@Table(name = "ai_evaluation")
public class AiEvaluation {

    @Id
    private UUID id;

    @Column(name = "grade_id", nullable = false, updatable = false)
    private UUID gradeId;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(nullable = false, updatable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AiOutcome outcome;

    @Column(updatable = false)
    private Boolean legible;

    @Column(updatable = false)
    private Integer confidence;

    @Column(name = "proposed_score", updatable = false)
    private BigDecimal proposedScore;

    @Column(updatable = false)
    private String feedback;

    @Column(updatable = false)
    private String transcription;

    @Column(updatable = false)
    private String error;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ai_evaluation_criterion", joinColumns = @JoinColumn(name = "ai_evaluation_id"))
    @OrderColumn(name = "position")
    private List<AiCriterionScore> criteria = new ArrayList<>();

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiEvaluation() {
    }

    private AiEvaluation(UUID gradeId, UUID rubricVersionId, String model, AiOutcome outcome, long durationMs,
            Instant now) {
        this.id = UUID.randomUUID();
        this.gradeId = gradeId;
        this.rubricVersionId = rubricVersionId;
        this.model = model;
        this.outcome = outcome;
        this.durationMs = durationMs;
        this.createdAt = now;
    }

    /** Evaluación válida del modelo, con la decisión de la política. */
    public static AiEvaluation completed(UUID gradeId, UUID rubricVersionId, String model,
            AiGradingPolicy.Decision decision, String transcription, String feedback, long durationMs, Instant now) {
        AiEvaluation evaluation = new AiEvaluation(gradeId, rubricVersionId, model, decision.outcome(), durationMs, now);
        evaluation.legible = decision.legible();
        evaluation.confidence = decision.confidence();
        evaluation.proposedScore = decision.score();
        evaluation.criteria = new ArrayList<>(decision.criteria());
        evaluation.transcription = transcription;
        evaluation.feedback = feedback;
        return evaluation;
    }

    /** Fallo técnico: el modelo no respondió o respondió algo no válido. No hay nota. */
    public static AiEvaluation failed(UUID gradeId, UUID rubricVersionId, String model, String error, long durationMs,
            Instant now) {
        AiEvaluation evaluation = new AiEvaluation(gradeId, rubricVersionId, model, AiOutcome.FAILED, durationMs, now);
        evaluation.error = error;
        return evaluation;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGradeId() {
        return gradeId;
    }

    public UUID getRubricVersionId() {
        return rubricVersionId;
    }

    public String getModel() {
        return model;
    }

    public AiOutcome getOutcome() {
        return outcome;
    }

    public Boolean getLegible() {
        return legible;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public BigDecimal getProposedScore() {
        return proposedScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public String getTranscription() {
        return transcription;
    }

    public String getError() {
        return error;
    }

    public List<AiCriterionScore> getCriteria() {
        return List.copyOf(criteria);
    }

    public long getDurationMs() {
        return durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
