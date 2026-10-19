package co.edu.unipamplona.logicode.identity.application;

import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;

/**
 * RN-16: la plataforma nunca puede quedar sin al menos una cuenta inicial activa con privilegio completo.
 */
final class FullPrivilegeGuard {

    private FullPrivilegeGuard() {
    }

    /** Llamar antes de una operación que haría que {@code target} deje de ser administrador activo con privilegio completo. */
    static void ensureAnotherRemains(UserAccountRepository accounts, UserAccount target) {
        if (target.isActiveFullAdmin() && accounts.countActiveFullAdmins() <= 1) {
            throw ApplicationException.businessRule(
                    "No se puede: la plataforma quedaría sin ninguna cuenta inicial activa con privilegios completos.");
        }
    }
}
