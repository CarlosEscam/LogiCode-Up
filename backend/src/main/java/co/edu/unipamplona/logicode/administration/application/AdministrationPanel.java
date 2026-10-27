package co.edu.unipamplona.logicode.administration.application;

import java.time.Clock;
import java.util.List;

import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Panel de administración (Etapa 8). El resumen lo ven las cuentas Administrador ({@code MANAGE_USERS}); los respaldos
 * los administran las mismas cuentas con {@code MANAGE_BACKUPS}, ordinarias o con privilegio completo (RF-28).
 */
@Service
public class AdministrationPanel {

    public record Backups(BackupStore.ServiceStatus service, boolean pendingRequest, List<BackupStore.Backup> items) {
    }

    private final PlatformStatistics statistics;
    private final BackupStore backups;
    private final Clock clock;

    public AdministrationPanel(PlatformStatistics statistics, BackupStore backups, Clock clock) {
        this.statistics = statistics;
        this.backups = backups;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PlatformOverview overview(Actor actor) {
        actor.require(Permission.MANAGE_USERS);
        return statistics.overview();
    }

    public Backups backups(Actor actor) {
        actor.require(Permission.MANAGE_BACKUPS);
        return new Backups(backups.status(), backups.hasPendingRequest(), backups.list());
    }

    /** Pide un respaldo inmediato; el servicio de respaldos lo toma en menos de un minuto. */
    public Backups requestBackup(Actor actor) {
        actor.require(Permission.MANAGE_BACKUPS);
        if (!backups.status().running()) {
            throw ApplicationException.conflict("El servicio de respaldos no está corriendo. Revisa el contenedor "
                    + "backup de Docker Compose (respaldos/README.md).");
        }
        if (backups.hasPendingRequest()) {
            throw ApplicationException.conflict("Ya hay un respaldo solicitado; espera a que termine.");
        }
        backups.request(actor.userId(), clock.instant());
        return backups(actor);
    }
}
