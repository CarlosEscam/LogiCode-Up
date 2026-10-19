package co.edu.unipamplona.logicode.identity.infrastructure.config;

import java.util.List;

import co.edu.unipamplona.logicode.identity.application.InitialAdmin;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de identidad ({@code logicode.identity}).
 *
 * @param publicUrl     dirección de la interfaz web para los enlaces de los correos
 * @param initialAdmins cuentas iniciales con privilegio completo (D-03), leídas de {@code INITIAL_ADMIN_*}
 */
@ConfigurationProperties(prefix = "logicode.identity")
record IdentityProperties(String publicUrl, List<InitialAdmin> initialAdmins) {

    IdentityProperties {
        initialAdmins = initialAdmins == null ? List.of() : List.copyOf(initialAdmins);
    }
}
