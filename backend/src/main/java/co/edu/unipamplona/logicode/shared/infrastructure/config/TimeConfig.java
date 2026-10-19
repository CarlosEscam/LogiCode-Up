package co.edu.unipamplona.logicode.shared.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del negocio en la zona horaria configurada (America/Bogota por defecto).
 * Los casos de uso reciben este {@link Clock} en lugar de llamar a {@code now()} directamente,
 * para que las reglas de fechas sean comprobables en pruebas.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    @Bean
    Clock businessClock(LogiCodeProperties properties) {
        return Clock.system(properties.timeZone());
    }
}
