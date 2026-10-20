package co.edu.unipamplona.logicode.grading.domain;

/**
 * {@code PROCESSING}: la IA evalúa la entrega. {@code PENDING_REVIEW}: en la bandeja Por revisar, esperando al docente.
 * {@code PUBLISHED}: nota visible para el estudiante.
 */
public enum GradeStatus {
    PROCESSING,
    PENDING_REVIEW,
    PUBLISHED
}
