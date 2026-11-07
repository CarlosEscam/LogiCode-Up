package co.edu.unipamplona.logicode.grading.infrastructure.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code logicode.ai.*}: calificación con IA (Ollama + Qwen2.5-VL 7B, RN-11). Apagada por defecto; ver README.
 *
 * @param ollamaUrl dirección del servicio Ollama ({@code http://ollama:11434} dentro de Docker Compose)
 * @param model etiqueta del modelo en Ollama
 * @param timeout tiempo máximo de una evaluación; en CPU una foto puede tardar minutos
 * @param keepAlive cuánto tiempo Ollama conserva el modelo en memoria entre evaluaciones
 * @param contextTokens ventana de contexto pedida al modelo (las imágenes consumen tokens)
 * @param maxImageSide lado mayor, en píxeles, al que se reducen las fotos antes de enviarlas
 * @param calibrationMinComparisons notas comparadas que hacen falta para cerrar la calibración de un curso
 * @param workerEnabled permite apagar el worker (las pruebas lo ejecutan a mano)
 */
@ConfigurationProperties(prefix = "logicode.ai")
record AiProperties(boolean enabled, String ollamaUrl, String model, Duration timeout, String keepAlive,
        int contextTokens, int maxImageSide, int maxAttempts, Duration retryDelay, Duration pollInterval,
        int calibrationMinComparisons, boolean workerEnabled) {
}
