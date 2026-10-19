package co.edu.unipamplona.logicode.identity.application;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import co.edu.unipamplona.logicode.shared.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lista de cuentas para la administración, con búsqueda por texto y filtros por rol y estado. */
@Service
public class ListUsers {

    private final UserAccountRepository accounts;

    public ListUsers(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public PageResult<UserAccount> execute(Actor actor, String text, Role role, AccountStatus status, int page, int size) {
        if (!actor.can(Permission.MANAGE_USERS) && !actor.can(Permission.APPROVE_TEACHERS)) {
            throw ApplicationException.forbidden();
        }
        return accounts.search(text == null || text.isBlank() ? null : text.trim(), role, status, Math.max(page, 0), Math.clamp(size, 1, 100));
    }
}
