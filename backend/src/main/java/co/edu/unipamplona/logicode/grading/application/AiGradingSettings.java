package co.edu.unipamplona.logicode.grading.application;

import java.time.Duration;

/**
 * Configuración de la calificación con IA ({@code logicode.ai.*}).
 *
 * @param enabled si las entregas nuevas se evalúan con IA; apagada, todo pasa a Por revisar
 * @param maxAttempts intentos ante fallos técnicos antes de pasar la entrega al docente
 * @param retryDelay espera base entre intentos; crece con cada intento
 * @param staleAfter tiempo tras el cual un trabajo en curso se considera abandonado y se retoma
 * @param calibrationMinComparisons notas comparadas con el docente que hacen falta para cerrar la calibración (T-90)
 */
public record AiGradingSettings(boolean enabled, int maxAttempts, Duration retryDelay, Duration staleAfter,
        int calibrationMinComparisons) {

    public AiGradingSettings {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("logicode.ai.max-attempts debe ser al menos 1");
        }
        if (calibrationMinComparisons < 1) {
            throw new IllegalArgumentException("logicode.ai.calibration-min-comparisons debe ser al menos 1");
        }
    }
}
