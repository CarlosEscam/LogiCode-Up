package co.edu.unipamplona.logicode.identity.application;

import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import co.edu.unipamplona.logicode.shared.domain.Role;

/**
 * Cuenta que ejecuta un caso de uso, con sus permisos efectivos calculados en el backend.
 */
public record Actor(UUID userId, String cedula, String fullName, Role role, boolean fullPrivileges,
        Set<Permission> permissions) {

    public static Actor of(UserAccount account) {
        return new Actor(account.getId(), account.getCedula(), account.getFullName(), account.getRole(),
                account.isFullPrivileges(), account.permissions());
    }

    public boolean can(Permission permission) {
        return permissions.contains(permission);
    }

    /** Lanza 403 si el actor no tiene el permiso. */
    public void require(Permission permission) {
        if (!can(permission)) {
            throw ApplicationException.forbidden();
        }
    }
}
