package co.edu.unipamplona.logicode.system.domain;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Información técnica de la plataforma en un instante dado.
 *
 * @param application   nombre de la aplicación
 * @param version       versión desplegada del backend
 * @param environment   entorno de ejecución
 * @param businessTime  hora actual en la zona horaria del negocio
 * @param schemaVersion última migración aplicada a la base de datos, o {@code null} si no hay ninguna
 */
public record SystemInfo(
        String application,
        String version,
        String environment,
        ZonedDateTime businessTime,
        String schemaVersion) {

    public SystemInfo {
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(businessTime, "businessTime");
    }
}
