package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-01, RN-01: registro de estudiante con cédula habilitada por el docente. */
@Service
public class RegisterStudent {

    public record Command(String cedula, String fullName, String email, String password) {
    }

    private final UserAccountRepository accounts;
    private final StudentEnablementRepository enablements;
    private final PasswordHasher passwordHasher;
    private final EmailVerificationIssuer verificationIssuer;
    private final AuditLog auditLog;
    private final Clock clock;

    public RegisterStudent(UserAccountRepository accounts, StudentEnablementRepository enablements,
            PasswordHasher passwordHasher, EmailVerificationIssuer verificationIssuer, AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
        this.enablements = enablements;
        this.passwordHasher = passwordHasher;
        this.verificationIssuer = verificationIssuer;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public UserAccount execute(Command command) {
        String cedula = RegistrationRules.requireCedula(command.cedula());
        String fullName = RegistrationRules.requireFullName(command.fullName());
        String email = RegistrationRules.requireEmail(command.email());
        RegistrationRules.requireValidPassword(command.password());

        StudentEnablement enablement = enablements.findByCedula(cedula)
                .orElseThrow(() -> ApplicationException.businessRule(
                        "Tu cédula no está habilitada. Pide a tu docente que la habilite para poder registrarte."));
        if (enablement.isRegistered() || accounts.existsByCedula(cedula)) {
            throw ApplicationException.conflict("Ya existe una cuenta con esta cédula.");
        }
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw ApplicationException.conflict("Este correo ya está registrado en otra cuenta.");
        }

        UserAccount account = accounts.save(UserAccount.registerStudent(
                cedula, fullName, email, passwordHasher.hash(command.password()), clock.instant()));
        enablement.markRegistered(account.getId());
        enablements.save(enablement);
        verificationIssuer.issue(account, email);
        auditLog.record(AuditAction.STUDENT_REGISTERED, account.getId(), account.getId(), cedula,
                AuditEvent.Outcome.SUCCESS, null);
        return account;
    }
}
