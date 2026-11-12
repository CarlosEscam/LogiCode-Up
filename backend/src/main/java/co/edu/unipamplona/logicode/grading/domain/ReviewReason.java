package co.edu.unipamplona.logicode.grading.domain;

/** Por qué una nota espera al docente en Por revisar (RF-15, RN-09). */
public enum ReviewReason {
    /** La calificación con IA estaba desactivada cuando llegó la entrega. */
    AI_DISABLED,
    /** La entrega trae un formato que la IA no lee (Word, video, Scratch, un PDF dañado o largo...). */
    UNSUPPORTED,
    /** El modelo indicó que la foto o el contenido no se puede leer. */
    ILLEGIBLE,
    /** La confianza reportada por el modelo quedó por debajo del 80 %. */
    LOW_CONFIDENCE,
    /** Falló el procesamiento técnico después de los reintentos; no hay nota. */
    AI_FAILED,
    /** La actividad tiene casos de prueba, pero el programa PSeInt no se pudo leer para ejecutarlo. */
    CODE_ERROR,
    /** El curso está en calibración: la IA propuso una nota que habría publicado sola, pero la confirma el docente. */
    CALIBRATION
}
