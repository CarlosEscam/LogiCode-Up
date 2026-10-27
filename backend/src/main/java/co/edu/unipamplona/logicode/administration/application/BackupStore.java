package co.edu.unipamplona.logicode.administration.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Carpeta de respaldos que llena el servicio {@code backup} de Docker Compose (ver respaldos/README.md). */
public interface BackupStore {

    /**
     * Respaldo terminado. {@code restoreTest} es {@code OK} o {@code ERROR} según la prueba de restauración que el
     * servicio hace después de cada copia, o nulo si no corrió.
     */
    record Backup(String name, Instant createdAt, long databaseBytes, long filesBytes, String restoreTest,
            String restoreTestDetail) {
    }

    /** Lo que el servicio informa: si está corriendo (latido reciente), a qué hora copia y cuántos días guarda. */
    record ServiceStatus(boolean running, Integer dailyHour, Integer retentionDays) {
    }

    List<Backup> list();

    ServiceStatus status();

    boolean hasPendingRequest();

    void request(UUID requestedBy, Instant now);
}
