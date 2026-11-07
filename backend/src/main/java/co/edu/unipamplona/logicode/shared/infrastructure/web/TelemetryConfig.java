package co.edu.unipamplona.logicode.shared.infrastructure.web;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Telemetría sin datos personales (guía 13, Ley 1581, T-99): la URL que queda en cada traza pasa por
 * {@link LogPaths} igual que la línea del log, y las lecturas de /actuator (Prometheus cada 15 s) no generan trazas.
 */
@Configuration(proxyBeanMethods = false)
class TelemetryConfig {

    @Bean
    ObservationFilter urlWithoutCedulas() {
        return context -> {
            if (context instanceof ServerRequestObservationContext request) {
                KeyValue url = request.getHighCardinalityKeyValue("http.url");
                if (url != null) {
                    request.addHighCardinalityKeyValue(KeyValue.of("http.url", LogPaths.sanitize(url.getValue())));
                }
            }
            return context;
        };
    }

    @Bean
    ObservationPredicate noActuatorTraces() {
        return (name, context) -> !(context instanceof ServerRequestObservationContext request
                && request.getCarrier().getRequestURI().startsWith("/actuator"));
    }
}
