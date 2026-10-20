package co.edu.unipamplona.logicode.grading.domain;

/** Resultado de una evaluación con IA. Solo {@code PUBLISHED} publica la nota. */
public enum AiOutcome {
    PUBLISHED,
    ILLEGIBLE,
    LOW_CONFIDENCE,
    FAILED
}
