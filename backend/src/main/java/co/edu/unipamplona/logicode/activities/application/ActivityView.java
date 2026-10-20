package co.edu.unipamplona.logicode.activities.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.ActivityKind;
import co.edu.unipamplona.logicode.activities.domain.ActivityStatus;
import co.edu.unipamplona.logicode.activities.domain.FileGroup;

/**
 * Actividad tal como la ve un actor. {@code progress} solo existe para un estudiante inscrito; {@code submittedCount}
 * (estudiantes con al menos una entrega) solo para quien ve las entregas del curso.
 */
public record ActivityView(UUID id, UUID courseId, String title, ActivityKind kind, String statement,
        boolean acceptsText, boolean acceptsFiles, boolean acceptsPhotos, Set<FileGroup> fileGroups,
        BigDecimal weightPercent, Instant opensAt, Instant closesAt, int maxAttempts, ActivityStatus status,
        boolean canManage, boolean canGrade, boolean canViewSubmissions, Integer submittedCount,
        StudentProgress progress) {

    /**
     * Situación del estudiante: {@code NOT_OPEN} (aún no abre), {@code OPEN} (puede entregar), {@code SUBMITTED}
     * (ya entregó), {@code OVERDUE} (venció sin entrega: cuenta como 0,0, RN-05).
     */
    public enum State { NOT_OPEN, OPEN, SUBMITTED, OVERDUE }

    public record StudentProgress(State state, int attemptsUsed, int attemptsAllowed, Instant deadline,
            boolean canSubmit, UUID lastSubmissionId, boolean hasException) {
    }
}
