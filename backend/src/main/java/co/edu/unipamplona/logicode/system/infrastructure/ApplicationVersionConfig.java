package co.edu.unipamplona.logicode.system.infrastructure;

import co.edu.unipamplona.logicode.shared.infrastructure.config.LogiCodeProperties;
import co.edu.unipamplona.logicode.system.application.ApplicationVersion;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ApplicationVersionConfig {

    @Bean
    ApplicationVersion applicationVersion(ObjectProvider<BuildProperties> buildProperties, LogiCodeProperties properties) {
        BuildProperties build = buildProperties.getIfAvailable();
        return new ApplicationVersion(build != null ? build.getVersion() : "dev", properties.environment());
    }
}
