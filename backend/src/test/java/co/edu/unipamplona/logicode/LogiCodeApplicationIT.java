package co.edu.unipamplona.logicode;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prueba de integración de la fundación: arranca la aplicación completa contra PostgreSQL real
 * (Testcontainers), aplica las migraciones y consulta los endpoints técnicos.
 * Se omite automáticamente si Docker no está disponible.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
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

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("10");

        String health = client.get().uri("/actuator/health").retrieve().body(String.class);
        assertThat(health).contains("\"status\":\"UP\"");

        String info = client.get().uri("/api/v1/system/info").retrieve().body(String.class);
        assertThat(info).contains("\"schemaVersion\":\"10\"").contains("\"timeZone\":\"America/Bogota\"");

        String openApi = client.get().uri("/api/v1/openapi").retrieve().body(String.class);
        assertThat(openApi).contains("/api/v1/system/info");
    }

    /**
     * Prueba de contrato del proveedor (guía 7, T-106): el contrato que publica el backend es el de
     * docs/api/openapi.json, que la interfaz verifica por su lado (frontend/src/test/contrato.test.js). Si un cambio lo
     * altera a propósito, se regenera con {@code mvn verify -Dlogicode.contrato.actualizar=true} y se revisa el diff.
     */
    @Test
    void publishedContractMatchesTheCommittedOne() throws Exception {
        String json = RestClient.create("http://localhost:" + port).get().uri("/api/v1/openapi").retrieve().body(String.class);
        JsonMapper mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
        Map<String, Object> contract = mapper.readValue(json, new TypeReference<TreeMap<String, Object>>() { });
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) contract.get("info");
        info.put("version", "ver GET /api/v1/system/info"); // cambia en cada versión sin cambiar el contrato
        String actual = mapper.writeValueAsString(contract) + "\n";

        Path committed = Path.of("..", "docs", "api", "openapi.json");
        if (Boolean.getBoolean("logicode.contrato.actualizar")) {
            Files.writeString(committed, actual);
        }
        assertThat(actual)
                .as("el contrato cambió: revisa el cambio y regenera docs/api/openapi.json (-Dlogicode.contrato.actualizar=true)")
                .isEqualTo(Files.readString(committed));
    }
}
