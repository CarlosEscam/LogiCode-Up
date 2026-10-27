package co.edu.unipamplona.logicode;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Prueba de integración de la fundación: arranca la aplicación completa contra PostgreSQL real
 * (Testcontainers), aplica las migraciones y consulta los endpoints técnicos.
 * Se omite automáticamente si Docker no está disponible.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class LogiCodeApplicationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private Flyway flyway;

    @Test
    void appliesMigrationsAndExposesHealthAndSystemInfo() {
        RestClient client = RestClient.create("http://localhost:" + port);

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("7");

        String health = client.get().uri("/actuator/health").retrieve().body(String.class);
        assertThat(health).contains("\"status\":\"UP\"");

        String info = client.get().uri("/api/v1/system/info").retrieve().body(String.class);
        assertThat(info).contains("\"schemaVersion\":\"7\"").contains("\"timeZone\":\"America/Bogota\"");

        String openApi = client.get().uri("/api/v1/openapi").retrieve().body(String.class);
        assertThat(openApi).contains("/api/v1/system/info");
    }
}
