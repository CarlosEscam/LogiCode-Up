package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-02, RN-02: solicitud de cuenta docente, pendiente hasta aprobación del administrador. */
@Service
public class RequestTeacherAccount {

    public record Command(String cedula, String fullName, String email, String password) {
    }

    private final UserAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final EmailVerificationIssuer verificationIssuer;
    private final AuditLog auditLog;
    private final Clock clock;

    public RequestTeacherAccount(UserAccountRepository accounts, PasswordHasher passwordHasher,
            EmailVerificationIssuer verificationIssuer, AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
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

        if (accounts.existsByCedula(cedula)) {
            throw ApplicationException.conflict("Ya existe una cuenta con esta cédula.");
        }
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw ApplicationException.conflict("Este correo ya está registrado en otra cuenta.");
        }

        UserAccount account = accounts.save(UserAccount.requestTeacher(
                cedula, fullName, email, passwordHasher.hash(command.password()), clock.instant()));
        verificationIssuer.issue(account, email);
        auditLog.record(AuditAction.TEACHER_REQUESTED, account.getId(), account.getId(), cedula,
                AuditEvent.Outcome.SUCCESS, null);
        return account;
    }
}
