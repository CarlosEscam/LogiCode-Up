package co.edu.unipamplona.logicode.identity.application;

import java.util.UUID;

/** Puerto para cerrar todas las sesiones activas de una cuenta (D-01). */
public interface SessionRevoker {

    void revokeAll(UUID userId);
}
