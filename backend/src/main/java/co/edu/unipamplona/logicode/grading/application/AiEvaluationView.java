package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiEvaluation;
import co.edu.unipamplona.logicode.grading.domain.AiOutcome;

/** Resultado de una evaluación con IA, para el docente del curso (RF-17). El estudiante no lo ve. */
public record AiEvaluationView(UUID id, String model, AiOutcome outcome, Boolean legible, Integer confidence,
        BigDecimal proposedScore, String feedback, String transcription, String error, List<Criterion> criteria,
        long durationMs, Instant createdAt) {

    public record Criterion(String name, BigDecimal points, BigDecimal maxPoints, String comment) {
    }

    static AiEvaluationView of(AiEvaluation e) {
        return new AiEvaluationView(e.getId(), e.getModel(), e.getOutcome(), e.getLegible(), e.getConfidence(),
                e.getProposedScore(), e.getFeedback(), e.getTranscription(), e.getError(),
                e.getCriteria().stream().map(c -> new Criterion(c.getName(), c.getPoints(), c.getMaxPoints(),
                        c.getComment())).toList(),
                e.getDurationMs(), e.getCreatedAt());
    }
}
