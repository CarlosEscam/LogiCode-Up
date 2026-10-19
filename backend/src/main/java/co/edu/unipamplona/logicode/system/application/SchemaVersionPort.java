package co.edu.unipamplona.logicode.system.application;

import java.util.Optional;

/**
 * Puerto para consultar la versión del esquema de base de datos. Lo implementa la capa de infraestructura.
 */
public interface SchemaVersionPort {

    Optional<String> currentVersion();
}
