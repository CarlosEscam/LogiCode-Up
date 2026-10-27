package co.edu.unipamplona.logicode.quizzes.infrastructure;

import co.edu.unipamplona.logicode.quizzes.application.TakeQuizzes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** RN-13: cada 30 s envía los intentos de quiz cuyo tiempo se agotó con el navegador cerrado. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "logicode.quizzes.sweeper-enabled", havingValue = "true", matchIfMissing = true)
class QuizDeadlineSweeper {

    private static final Logger log = LoggerFactory.getLogger(QuizDeadlineSweeper.class);

    private final TakeQuizzes takeQuizzes;

    QuizDeadlineSweeper(TakeQuizzes takeQuizzes) {
        this.takeQuizzes = takeQuizzes;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    void finishExpiredAttempts() {
        try {
            int finished = takeQuizzes.finishExpired();
            if (finished > 0) {
                log.info("Se enviaron {} intentos de quiz con el tiempo agotado", finished);
            }
        } catch (RuntimeException e) {
            log.error("Falló la revisión de intentos de quiz vencidos; se reintenta en la siguiente", e);
        }
    }
}
