package co.edu.unipamplona.logicode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Etapa 9: recorre todos los endpoints de la API sin sesión. Solo los públicos aprobados responden algo distinto de
 * 401, y ninguna escritura pasa sin token CSRF. Un endpoint nuevo queda cubierto sin escribir otra prueba.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiSecurityIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    /** Lo que un visitante puede pedir (RF-24, RF-25, RF-26, documentación de la API). Cambiarlo es una decisión de seguridad. */
    static final Set<String> PUBLIC = Set.of(
            "GET /api/v1/system/info", "GET /api/v1/openapi", "GET /api/v1/openapi/swagger-config",
            "GET /api/v1/library", "GET /api/v1/library/topics",
            "GET /api/v1/materials/{materialId}", "GET /api/v1/materials/{materialId}/preview",
            "GET /api/v1/materials/{materialId}/download",
            "GET /api/v1/forum/threads", "GET /api/v1/forum/threads/{threadId}");

    @Autowired
    MockMvc mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Test
    void onlyApprovedEndpointsAnswerWithoutSession() throws Exception {
        Set<String> open = new TreeSet<>();
        for (String endpoint : endpoints()) {
            if (endpoint.contains(" /api/v1/auth/")) {
                continue; // registro, inicio de sesión y recuperación: públicos por definición
            }
            String[] parts = endpoint.split(" ");
            int status = mvc.perform(request(HttpMethod.valueOf(parts[0]), concrete(parts[1])).with(csrf()))
                    .andReturn().getResponse().getStatus();
            if (status != 401) {
                open.add(endpoint);
            }
        }
        assertThat(open).as("endpoints que responden sin sesión").isEqualTo(new TreeSet<>(PUBLIC));
    }

    @Test
    void everyWriteNeedsCsrfToken() throws Exception {
        List<String> unprotected = new ArrayList<>();
        for (String endpoint : endpoints()) {
            String[] parts = endpoint.split(" ");
            if (parts[0].equals("GET")) {
                continue;
            }
            int status = mvc.perform(request(HttpMethod.valueOf(parts[0]), concrete(parts[1])))
                    .andReturn().getResponse().getStatus();
            if (status != 403) {
                unprotected.add(endpoint + " → " + status);
            }
        }
        assertThat(unprotected).as("escrituras sin CSRF").isEmpty();
    }

    @Test
    void responsesCarrySecurityHeaders() throws Exception {
        var response = mvc.perform(get("/api/v1/system/info")).andReturn().getResponse();
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
    }

    private List<String> endpoints() {
        List<String> all = new ArrayList<>();
        mappings.getHandlerMethods().keySet().forEach(info -> info.getPathPatternsCondition().getPatternValues()
                .stream().filter(p -> p.startsWith("/api/v1/"))
                .forEach(p -> {
                    Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                    (methods.isEmpty() ? Set.of(RequestMethod.GET) : methods).forEach(m -> all.add(m + " " + p));
                }));
        assertThat(all).as("endpoints encontrados").hasSizeGreaterThan(50);
        return all;
    }

    private static String concrete(String pattern) {
        return pattern.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
    }
}
