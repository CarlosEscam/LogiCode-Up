package co.edu.unipamplona.logicode.system.api;

import java.time.OffsetDateTime;

import co.edu.unipamplona.logicode.system.domain.SystemInfo;

/**
 * Respuesta JSON de {@code GET /api/v1/system/info}.
 */
public record SystemInfoResponse(
        String application,
        String version,
        String environment,
        OffsetDateTime serverTime,
        String timeZone,
        String schemaVersion) {

    static SystemInfoResponse from(SystemInfo info) {
        return new SystemInfoResponse(
                info.application(),
                info.version(),
                info.environment(),
                info.businessTime().toOffsetDateTime(),
                info.businessTime().getZone().getId(),
                info.schemaVersion());
    }
}
