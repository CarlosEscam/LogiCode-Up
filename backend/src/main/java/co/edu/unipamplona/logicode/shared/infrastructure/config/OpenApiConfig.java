package co.edu.unipamplona.logicode.shared.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI logiCodeOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        String version = buildProperties.getIfAvailable() != null
                ? buildProperties.getIfAvailable().getVersion()
                : "dev";
        return new OpenAPI().info(new Info()
                .title("LogiCode API")
                .version(version)
                .description("API REST de LogiCode, plataforma de Pensamiento Computacional de la Universidad de Pamplona. "
                        + "Todas las rutas de negocio usan el prefijo /api/v1."));
    }
}
