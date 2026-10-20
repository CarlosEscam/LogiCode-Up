package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto hacia el modelo que califica (RN-08, RN-18). Recibe el nombre del estudiante, el contenido entregado y el
 * contexto académico; nunca la cédula, el correo ni los nombres originales de los archivos. Los adaptadores traducen
 * a cada proveedor (hoy Ollama + Qwen2.5-VL 7B) sin tocar las reglas de publicación.
 */
public interface GradingProvider {

    /** Identificador del modelo, guardado con cada evaluación. */
    String model();

    /**
     * Evalúa la entrega con la rúbrica.
     *
     * @throws GradingFailure si el proveedor falla, no responde o devuelve algo que no se puede interpretar
     */
    Result evaluate(Request request);

    record Request(String studentName, String activityTitle, String statement, String expectedAnswer,
            List<Criterion> criteria, String textAnswer, List<TextFile> textFiles, List<Image> images) {
    }

    record Criterion(String name, String description, BigDecimal maxPoints) {
    }

    /** Archivo de texto (PSeInt, Arduino) con una etiqueta neutra en lugar de su nombre original. */
    record TextFile(String label, String format, String content) {
    }

    /** Foto o imagen entregada (PNG o JPG). */
    record Image(String label, byte[] content) {
    }

    /** Respuesta estructurada: lo leído, legibilidad, puntos por criterio en el orden de la rúbrica y confianza. */
    record Result(String transcription, boolean legible, List<CriterionResult> criteria, String feedback,
            int confidence) {
    }

    record CriterionResult(BigDecimal points, String comment) {
    }

    /** Fallo técnico del proveedor. No produce nota (RF-15, RNF-06). */
    class GradingFailure extends RuntimeException {

        public GradingFailure(String message) {
            super(message);
        }

        public GradingFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
