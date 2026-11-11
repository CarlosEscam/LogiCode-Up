package co.edu.unipamplona.logicode.identity.infrastructure.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Límite por dirección IP en las rutas que un atacante puede repetir sin cuenta o que mandan correos: intentos de
 * ingreso fallidos repartidos entre muchas cédulas, solicitudes de docente en ráfaga y reenvíos de verificación
 * (cada uno gasta un correo de Brevo). Complementa el bloqueo por cédula de {@code AuthenticateUser}.
 * <p>
 * Los topes son altos a propósito: en Docker Desktop todo el salón puede llegar con la misma IP.
 * shortcut: cuenta en memoria de este backend y se reinicia al reiniciarlo; con varias réplicas habría que moverlo a
 * PostgreSQL.
 */
class AbuseLimitFilter extends OncePerRequestFilter {

    /** {@code onlyFailures}: solo cuentan las respuestas 401 (intentos de ingreso fallidos). */
    record Rule(String path, int limit, Duration window, boolean onlyFailures) {
    }

    static final List<Rule> RULES = List.of(
            new Rule("/api/v1/auth/login", 50, Duration.ofMinutes(15), true),
            new Rule("/api/v1/auth/teacher-requests", 10, Duration.ofHours(1), false),
            new Rule("/api/v1/auth/password-recovery", 30, Duration.ofHours(1), false),
            new Rule("/api/v1/auth/email-change-requests", 30, Duration.ofHours(1), false),
            new Rule("/api/v1/me/email-verification", 10, Duration.ofHours(1), false));

    private static final int MAX_KEYS = 50_000;

    private final Clock clock;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    AbuseLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = "POST".equals(request.getMethod()) ? RULES.stream()
                .filter(r -> r.path().equals(request.getRequestURI())).findFirst().orElse(null) : null;
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }
        Instant now = clock.instant();
        String key = rule.path() + '|' + request.getRemoteAddr();
        if (count(key, now.minus(rule.window())) >= rule.limit()) {
            SecurityConfig.problem(response, 429, "Demasiadas solicitudes",
                    "Se hicieron demasiadas solicitudes desde este equipo. Espera un rato y vuelve a intentarlo.");
            return;
        }
        chain.doFilter(request, response);
        if (!rule.onlyFailures() || response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            if (hits.size() > MAX_KEYS) {
                hits.clear(); // un ataque con miles de IP distintas no llena la memoria
            }
            Deque<Instant> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (times) {
                times.addLast(now);
            }
        }
    }

    private int count(String key, Instant since) {
        Deque<Instant> times = hits.get(key);
        if (times == null) {
            return 0;
        }
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(since)) {
                times.pollFirst();
            }
            return times.size();
        }
    }
}
