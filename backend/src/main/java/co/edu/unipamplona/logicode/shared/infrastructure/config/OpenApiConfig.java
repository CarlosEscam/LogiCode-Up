package co.edu.unipamplona.logicode.shared.infrastructure.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI (guía 6, T-105). La seguridad es la real: sesión en el servidor con la cookie LOGICODE_SESSION
 * (HttpOnly) y token CSRF en la cabecera X-XSRF-TOKEN para escribir, no JWT (decisión de la Etapa 3: poder cerrar
 * todas las sesiones de una persona). ApiSecurityIT comprueba que las operaciones sin sesión de este documento sean
 * exactamente las que responden sin sesión.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    static final String SESSION = "sesion";
    static final String CSRF = "csrf";
    static final String PROBLEM = "Problema";
    private static final List<String> PUBLIC_GET_PREFIXES = List.of("/api/v1/library", "/api/v1/materials/",
            "/api/v1/forum/");

    @Bean
    OpenAPI logiCodeOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        String version = buildProperties.getIfAvailable() != null
                ? buildProperties.getIfAvailable().getVersion()
                : "dev";
        return new OpenAPI().servers(List.of(new Server().url("/").description("El mismo origen de la interfaz")))
                .info(new Info()
                        .title("LogiCode API")
                        .version(version)
                        .description("API REST de LogiCode, plataforma de Pensamiento Computacional de la Universidad de Pamplona. "
                                + "Todas las rutas de negocio usan el prefijo /api/v1. La sesión se abre con POST /auth/login "
                                + "y viaja en la cookie LOGICODE_SESSION; toda escritura lleva además el valor de la cookie "
                                + "XSRF-TOKEN en la cabecera X-XSRF-TOKEN."))
                .components(new Components()
                        .addSecuritySchemes(SESSION, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("LOGICODE_SESSION")
                                .description("Sesión guardada en el servidor (PostgreSQL); cookie HttpOnly y SameSite=Lax"))
                        .addSecuritySchemes(CSRF, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-XSRF-TOKEN")
                                .description("Valor de la cookie XSRF-TOKEN; obligatorio en POST, PUT, PATCH y DELETE")));
    }

    // Problem Details (RFC 9457) como los arma GlobalExceptionHandler; `errors` solo en los de validación.
    private static Schema<?> problemSchema() {
        Schema<?> fieldError = new ObjectSchema()
                .addProperty("field", new StringSchema())
                .addProperty("message", new StringSchema());
        return new ObjectSchema().description("Error en formato Problem Details (RFC 9457), con mensajes en español")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Regla de negocio incumplida"))
                .addProperty("status", new IntegerSchema().example(422))
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty("errors", new ArraySchema().items(fieldError));
    }

    private static void addProblem(Operation op, String status, String description) {
        Content content = new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM)));
        op.getResponses().putIfAbsent(status, new ApiResponse().description(description).content(content));
    }

    /** Lo público no pide sesión; las consultas piden sesión; toda escritura pide CSRF (también el inicio de sesión). */
    @Bean
    OpenApiCustomizer securityPerOperation() {
        return openApi -> {
            // Aquí y no en el bean OpenAPI: springdoc arma de nuevo los esquemas a partir de los controladores.
            openApi.getComponents().addSchemas(PROBLEM, problemSchema());
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
            SecurityRequirement requirement = new SecurityRequirement();
            if (!isPublic(path, method)) {
                requirement.addList(SESSION);
            }
            if (method != PathItem.HttpMethod.GET) {
                requirement.addList(CSRF);
            }
            op.setSecurity(requirement.isEmpty() ? List.of() : List.of(requirement));
            // Errores comunes a toda operación según lo que recibe; los 409 y 422 propios de cada caso no se listan.
            if (op.getRequestBody() != null || op.getParameters() != null) {
                addProblem(op, "400", "Datos con formato inválido");
            }
            if (requirement.containsKey(SESSION)) {
                addProblem(op, "401", "Sin sesión");
            }
            if (!requirement.isEmpty()) {
                addProblem(op, "403", "Sin permiso, o escritura sin token CSRF");
            }
            if (path.contains("{")) {
                addProblem(op, "404", "El recurso no existe");
            }
            }));
        };
    }

    // Igual que SecurityConfig. Registro, inicio de sesión y recuperación son públicos; GET /me responde 401 sin sesión.
    static boolean isPublic(String path, PathItem.HttpMethod method) {
        if (path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/system/")) {
            return true;
        }
        return method == PathItem.HttpMethod.GET && PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
