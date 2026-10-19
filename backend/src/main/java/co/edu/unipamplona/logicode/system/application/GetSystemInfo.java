package co.edu.unipamplona.logicode.system.application;

import java.time.Clock;
import java.time.ZonedDateTime;

import co.edu.unipamplona.logicode.system.domain.SystemInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: obtener la información técnica de la plataforma.
 */
@Service
public class GetSystemInfo {

    public static final String APPLICATION_NAME = "LogiCode";

    private final SchemaVersionPort schemaVersion;
    private final Clock clock;
    private final String version;
    private final String environment;

    public GetSystemInfo(SchemaVersionPort schemaVersion, Clock clock, ApplicationVersion applicationVersion) {
        this.schemaVersion = schemaVersion;
        this.clock = clock;
        this.version = applicationVersion.version();
        this.environment = applicationVersion.environment();
    }

    @Transactional(readOnly = true)
    public SystemInfo execute() {
        return new SystemInfo(
                APPLICATION_NAME,
                version,
                environment,
                ZonedDateTime.now(clock),
                schemaVersion.currentVersion().orElse(null));
    }
}
