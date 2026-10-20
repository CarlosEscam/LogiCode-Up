package co.edu.unipamplona.logicode.activities.application;

import java.util.UUID;

/**
 * Lo que este módulo necesita de la evaluación. Lo implementa el módulo {@code grading}, que conoce las rúbricas y
 * las notas; así la dependencia va en un solo sentido (grading → activities).
 */
public interface GradingGateway {

    /** RN-20: una actividad se publica solo con su rúbrica definida. */
    boolean hasRubric(UUID activityId);

    /** Registra la entrega para evaluar con la versión de rúbrica vigente en este momento (RN-20). */
    void submissionReceived(UUID submissionId, UUID activityId);

    /** El enunciado cambió: las entregas siguientes se evalúan con una versión nueva de la rúbrica. */
    void statementChanged(UUID activityId, String statement, UUID changedBy);
}
