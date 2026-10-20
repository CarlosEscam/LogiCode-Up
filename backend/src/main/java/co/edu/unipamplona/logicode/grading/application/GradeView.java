package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.GradeSource;
import co.edu.unipamplona.logicode.grading.domain.GradeStatus;

/** Nota de una entrega. Para el estudiante, nota y retroalimentación solo existen cuando está publicada. */
public record GradeView(UUID id, UUID submissionId, GradeStatus status, BigDecimal score, String feedback,
        GradeSource source, String gradedByName, Instant gradedAt, int rubricVersion) {
}
