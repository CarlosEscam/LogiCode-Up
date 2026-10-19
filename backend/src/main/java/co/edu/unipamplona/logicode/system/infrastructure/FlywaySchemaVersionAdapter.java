package co.edu.unipamplona.logicode.system.infrastructure;

import java.util.Optional;

import co.edu.unipamplona.logicode.system.application.SchemaVersionPort;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.stereotype.Component;

/**
 * Obtiene la versión del esquema desde el historial de migraciones de Flyway.
 */
@Component
class FlywaySchemaVersionAdapter implements SchemaVersionPort {

    private final Flyway flyway;

    FlywaySchemaVersionAdapter(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public Optional<String> currentVersion() {
        MigrationInfo current = flyway.info().current();
        return Optional.ofNullable(current)
                .map(MigrationInfo::getVersion)
                .map(Object::toString);
    }
}
