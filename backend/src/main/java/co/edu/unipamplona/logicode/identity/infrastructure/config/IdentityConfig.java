package co.edu.unipamplona.logicode.identity.infrastructure.config;

import co.edu.unipamplona.logicode.identity.application.EnsureInitialAdmins;
import co.edu.unipamplona.logicode.identity.application.IdentitySettings;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class IdentityConfig {

    @Bean
    IdentitySettings identitySettings(IdentityProperties properties) {
        return new IdentitySettings(properties.publicUrl());
    }

    /** Al arrancar, asegura las cuentas iniciales con privilegio completo (RN-22). */
    @Bean
    ApplicationRunner initialAdminsRunner(IdentityProperties properties, EnsureInitialAdmins ensureInitialAdmins) {
        return args -> ensureInitialAdmins.execute(properties.initialAdmins());
    }
}
