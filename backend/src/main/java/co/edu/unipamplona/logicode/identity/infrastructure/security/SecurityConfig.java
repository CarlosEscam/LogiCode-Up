package co.edu.unipamplona.logicode.identity.infrastructure.security;

import java.io.IOException;
import java.time.Clock;

import co.edu.unipamplona.logicode.identity.application.GetCurrentActor;
import co.edu.unipamplona.logicode.identity.application.TrustedDevices;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Seguridad HTTP: sesión en servidor (Spring Session JDBC, D-01) con cookie HttpOnly, protección CSRF con cookie
 * {@code XSRF-TOKEN} para la interfaz React, y respuestas 401/403 en formato Problem Details.
 * <p>
 * Aquí solo se exige sesión (salvo la biblioteca pública); los permisos finos (rol, privilegio completo) los verifican los casos de uso.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository,
            GetCurrentActor getCurrentActor, TrustedDevices trustedDevices, Clock clock) throws Exception {
        http
                .addFilterAfter(new ActiveAccountFilter(getCurrentActor), SecurityContextHolderFilter.class)
                .addFilterAfter(new AbuseLimitFilter(clock, trustedDevices), ActiveAccountFilter.class)
                .csrf(csrf -> csrf.spa())
                // La vista previa de PDF se muestra en un iframe de la propia aplicación.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        // metrics y prometheus: el puerto del backend solo escucha en 127.0.0.1 y Nginx no publica /actuator.
                        .requestMatchers("/actuator/health/**", "/actuator/health", "/actuator/info",
                                "/actuator/prometheus", "/actuator/metrics", "/actuator/metrics/**").permitAll()
                        .requestMatchers("/api/v1/openapi/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/system/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/me").permitAll()
                        // Biblioteca pública (RF-24, RF-25): los casos de uso deciden qué ve un visitante.
                        .requestMatchers(HttpMethod.GET, "/api/v1/library", "/api/v1/library/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/materials/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/forum/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        // Lo que no está en esta lista se niega (antes se permitía).
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> problem(response,
                                HttpServletResponse.SC_UNAUTHORIZED, "Sesión requerida", "Debes iniciar sesión."))
                        .accessDeniedHandler((request, response, ex) -> problem(response,
                                HttpServletResponse.SC_FORBIDDEN, "Acceso denegado",
                                "La solicitud fue rechazada. Recarga la página e inténtalo de nuevo.")));
        return http.build();
    }

    static void problem(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\"}");
    }
}
