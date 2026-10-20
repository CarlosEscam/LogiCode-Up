package co.edu.unipamplona.logicode.grading.infrastructure.ai;

import co.edu.unipamplona.logicode.grading.application.RunAiGrading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisa la cola de evaluación cada {@code logicode.ai.poll-interval} y procesa los trabajos listos de a uno: en CPU
 * el modelo no gana nada evaluando en paralelo. Solo existe con la IA activada.
 */
@Component
@ConditionalOnExpression("${logicode.ai.enabled:false} and ${logicode.ai.worker-enabled:true}")
class AiGradingWorker {

    private static final Logger log = LoggerFactory.getLogger(AiGradingWorker.class);

    private final RunAiGrading runAiGrading;

    AiGradingWorker(RunAiGrading runAiGrading) {
        this.runAiGrading = runAiGrading;
    }

    @Scheduled(initialDelayString = "${logicode.ai.poll-interval}", fixedDelayString = "${logicode.ai.poll-interval}")
    void drainQueue() {
        try {
            while (runAiGrading.processNext()) {
                // Sigue mientras haya trabajos listos.
            }
        } catch (RuntimeException e) {
            log.error("Falló una vuelta del worker de calificación con IA; se reintenta en la siguiente", e);
        }
    }
}
