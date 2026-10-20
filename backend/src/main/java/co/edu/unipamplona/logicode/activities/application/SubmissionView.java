package co.edu.unipamplona.logicode.activities.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.files.application.StoredFileInfo;

/** Un intento de entrega con sus archivos. La nota la entrega el módulo grading. */
public record SubmissionView(UUID id, UUID activityId, String activityTitle, UUID courseId, UUID studentId,
        String studentName, int attempt, String textAnswer, List<StoredFileInfo> files, Instant submittedAt,
        boolean late) {
}
