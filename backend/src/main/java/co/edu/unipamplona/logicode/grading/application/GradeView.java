package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.GradeSource;
import co.edu.unipamplona.logicode.grading.domain.GradeStatus;
import co.edu.unipamplona.logicode.grading.domain.ReviewReason;

/**
 * Nota de una entrega. Para el estudiante, nota y retroalimentación solo existen cuando está publicada, y no ve el
 * motivo por el que una nota espera al docente.
 */
public record GradeView(UUID id, UUID submissionId, GradeStatus status, ReviewReason reviewReason, BigDecimal score,
        String feedback, GradeSource source, String gradedByName, Instant gradedAt, int rubricVersion) {
}
