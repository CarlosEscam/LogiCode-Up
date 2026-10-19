package co.edu.unipamplona.logicode.identity.application;

import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.Role;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.PageResult;

/** Puerto de persistencia de cuentas. */
public interface UserAccountRepository {

    UserAccount save(UserAccount account);

    Optional<UserAccount> findById(UUID id);

    Optional<UserAccount> findByCedula(String cedula);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByCedula(String cedula);

    boolean existsByEmailIgnoreCase(String email);

    /** Cuentas activas con rol Administrador y privilegio completo (RN-16). */
    long countActiveFullAdmins();

    PageResult<UserAccount> search(Role role, AccountStatus status, int page, int size);

    java.util.List<UserAccount> findAllWithFullPrivileges();
}
