package co.edu.unipamplona.logicode.identity.infrastructure.security;

import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.SessionRevoker;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Cierra todas las sesiones de una cuenta borrándolas de PostgreSQL (D-01). El nombre de principal de cada sesión
 * es el identificador de la cuenta.
 */
@Component
class SpringSessionRevoker implements SessionRevoker {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    SpringSessionRevoker(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void revokeAll(UUID userId) {
        sessions.findByPrincipalName(userId.toString()).keySet().forEach(sessions::deleteById);
    }
}
