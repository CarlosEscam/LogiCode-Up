package co.edu.unipamplona.logicode.identity.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de personas para otros módulos: inscritos de un curso, nombre del docente responsable. */
@Service
public class UserDirectory {

    /** Nombre conocido (de la cuenta o, si aún no se registra, de la habilitación) y si ya tiene cuenta. */
    public record StudentInfo(String cedula, String fullName, boolean registered) {
    }

    private final StudentEnablementRepository enablements;
    private final UserAccountRepository accounts;

    public UserDirectory(StudentEnablementRepository enablements, UserAccountRepository accounts) {
        this.enablements = enablements;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Map<String, StudentInfo> lookup(Collection<String> cedulas) {
        Map<String, StudentInfo> result = new HashMap<>();
        for (String cedula : cedulas) {
            UserAccount account = accounts.findByCedula(cedula).orElse(null);
            String name = account != null ? account.getFullName()
                    : enablements.findByCedula(cedula).map(StudentEnablement::getFullName).orElse(null);
            result.put(cedula, new StudentInfo(cedula, name, account != null));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<String> fullNameOf(UUID userId) {
        return accounts.findById(userId).map(UserAccount::getFullName);
    }

    /** Todas las cédulas habilitadas, para inscribirlas en el primer curso que se cree. */
    @Transactional(readOnly = true)
    public List<String> allEnabledCedulas() {
        return enablements.findAllCedulas();
    }
}
