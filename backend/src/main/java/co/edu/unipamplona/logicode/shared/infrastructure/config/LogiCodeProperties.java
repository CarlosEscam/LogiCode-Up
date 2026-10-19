package co.edu.unipamplona.logicode.shared.infrastructure.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración propia de LogiCode (prefijo {@code logicode}).
 *
 * @param environment nombre del entorno en ejecución (local, docker, ...), solo informativo
 * @param timeZone    zona horaria del negocio; las reglas por "día calendario en Colombia" la usan
 */
@ConfigurationProperties(prefix = "logicode")
public record LogiCodeProperties(String environment, ZoneId timeZone) {

    public LogiCodeProperties {
        if (environment == null || environment.isBlank()) {
            environment = "local";
        }
        if (timeZone == null) {
            timeZone = ZoneId.of("America/Bogota");
        }
    }
}
