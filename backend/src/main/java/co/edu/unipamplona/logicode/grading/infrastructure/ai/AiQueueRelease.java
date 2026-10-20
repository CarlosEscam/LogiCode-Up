package co.edu.unipamplona.logicode.grading.infrastructure.ai;

import co.edu.unipamplona.logicode.grading.application.RunAiGrading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Con la IA desactivada al arrancar, las entregas que quedaron en cola pasan al docente en lugar de esperar. */
@Component
@ConditionalOnProperty(prefix = "logicode.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
class AiQueueRelease {

    private static final Logger log = LoggerFactory.getLogger(AiQueueRelease.class);

    private final RunAiGrading runAiGrading;

    AiQueueRelease(RunAiGrading runAiGrading) {
        this.runAiGrading = runAiGrading;
    }

    @EventListener(ApplicationReadyEvent.class)
    void releaseQueue() {
        int released = runAiGrading.releaseQueue();
        if (released > 0) {
            log.info("Calificación con IA desactivada: {} entregas en cola pasaron a Por revisar", released);
        }
    }
}
