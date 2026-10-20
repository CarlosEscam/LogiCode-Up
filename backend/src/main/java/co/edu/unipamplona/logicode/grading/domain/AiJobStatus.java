package co.edu.unipamplona.logicode.grading.domain;

/** Estado de un trabajo de la cola de evaluación con IA. */
public enum AiJobStatus {
    PENDING,
    RUNNING,
    DONE,
    FAILED
}
