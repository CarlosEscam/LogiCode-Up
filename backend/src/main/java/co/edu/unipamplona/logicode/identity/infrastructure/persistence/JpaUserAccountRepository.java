package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface JpaUserAccountRepository extends JpaRepository<UserAccount, UUID>, JpaSpecificationExecutor<UserAccount> {

    java.util.Optional<UserAccount> findByCedula(String cedula);

    java.util.Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByCedula(String cedula);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select count(a) from UserAccount a where a.role = :role and a.status = :status and a.fullPrivileges = true")
    long countFullAdmins(Role role, AccountStatus status);

    List<UserAccount> findByFullPrivilegesTrue();
}
