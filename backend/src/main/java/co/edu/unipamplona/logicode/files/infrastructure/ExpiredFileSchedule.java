package co.edu.unipamplona.logicode.files.infrastructure;

import co.edu.unipamplona.logicode.files.application.ExpiredFileCleanup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Todos los días a las 3:30 a. m. (hora de Colombia) borra los archivos vencidos que nadie usa. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class ExpiredFileSchedule {

    private static final Logger log = LoggerFactory.getLogger(ExpiredFileSchedule.class);

    private final ExpiredFileCleanup cleanup;

    ExpiredFileSchedule(ExpiredFileCleanup cleanup) {
        this.cleanup = cleanup;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "${logicode.time-zone}")
    void deleteExpiredFiles() {
        try {
            int deleted = cleanup.deleteExpired();
            if (deleted > 0) {
                log.info("Se borraron {} archivos que cumplieron su tiempo de conservación", deleted);
            }
        } catch (RuntimeException e) {
            log.error("Falló el borrado de archivos vencidos; se reintenta mañana", e);
        }
    }
}
