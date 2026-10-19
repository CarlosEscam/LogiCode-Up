package co.edu.unipamplona.logicode.identity.infrastructure.security;

import java.io.IOException;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.GetCurrentActor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * En cada solicitud con sesión, vuelve a cargar la cuenta: si fue desactivada o ya no existe, la sesión se
 * invalida y la solicitud continúa como anónima. Así los cambios de estado tienen efecto inmediato.
 */
class ActiveAccountFilter extends OncePerRequestFilter {

    static final String ACTOR_ATTRIBUTE = Actor.class.getName();

    private final GetCurrentActor getCurrentActor;

    ActiveAccountFilter(GetCurrentActor getCurrentActor) {
        this.getCurrentActor = getCurrentActor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof String principal) {
            UUID userId = parse(principal);
            var actor = userId == null ? java.util.Optional.<Actor>empty() : getCurrentActor.byUserId(userId);
            if (actor.isPresent()) {
                request.setAttribute(ACTOR_ATTRIBUTE, actor.get());
            } else {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static UUID parse(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
