package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.UserAccountRepository;
import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.Role;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class UserAccountRepositoryAdapter implements UserAccountRepository {

    private final JpaUserAccountRepository jpa;

    UserAccountRepositoryAdapter(JpaUserAccountRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public UserAccount save(UserAccount account) {
        return jpa.save(account);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<UserAccount> findByCedula(String cedula) {
        return jpa.findByCedula(cedula);
    }

    @Override
    public Optional<UserAccount> findByEmailIgnoreCase(String email) {
        return jpa.findByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByCedula(String cedula) {
        return jpa.existsByCedula(cedula);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return jpa.existsByEmailIgnoreCase(email);
    }

    @Override
    public long countActiveFullAdmins() {
        return jpa.countFullAdmins(Role.ADMIN, AccountStatus.ACTIVE);
    }

    @Override
    public PageResult<UserAccount> search(String text, Role role, AccountStatus status, int page, int size) {
        Specification<UserAccount> spec = (root, query, cb) -> cb.conjunction();
        if (text != null) {
            String pattern = "%" + text.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(root.get("cedula"), pattern, '\\'),
                    cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("email")), pattern, '\\')));
        }
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return PageResults.of(jpa.findAll(spec, PageRequest.of(page, size, Sort.by("fullName").ascending())));
    }

    @Override
    public List<UserAccount> findAllWithFullPrivileges() {
        return jpa.findByFullPrivilegesTrue();
    }
}
