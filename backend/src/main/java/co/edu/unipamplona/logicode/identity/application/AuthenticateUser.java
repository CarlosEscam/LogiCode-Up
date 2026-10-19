package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.Cedula;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-03: inicio de sesión con cédula y contraseña. Los fallos devuelven un mensaje genérico y se limitan a
 * {@value #MAX_FAILURES} intentos fallidos por cédula cada 15 minutos (controles contra abuso).
 */
@Service
public class AuthenticateUser {

    static final int MAX_FAILURES = 5;
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    static final String INVALID_CREDENTIALS = "Cédula o contraseña incorrectas.";

    private final UserAccountRepository accounts;
    private final AuditEventRepository auditEvents;
    private final PasswordHasher passwordHasher;
    private final AuditLog auditLog;
    private final Clock clock;

    public AuthenticateUser(UserAccountRepository accounts, AuditEventRepository auditEvents,
            PasswordHasher passwordHasher, AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
        this.auditEvents = auditEvents;
        this.passwordHasher = passwordHasher;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    /** Devuelve el actor autenticado; lanza 401, 403 o 429 según el caso. El intento queda auditado. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApplicationException.class)
    public Actor execute(String rawCedula, String password) {
        String cedula = Optional.ofNullable(Cedula.normalize(rawCedula)).orElse("");
        long recentFailures = auditEvents.countByActionAndSubjectAndOutcomeSince(
                AuditAction.LOGIN, cedula, AuditEvent.Outcome.FAILURE, clock.instant().minus(FAILURE_WINDOW));
        if (recentFailures >= MAX_FAILURES) {
            auditLog.record(AuditAction.LOGIN, null, null, cedula, AuditEvent.Outcome.LIMIT_REACHED, null);
            throw ApplicationException.tooManyRequests(
                    "Demasiados intentos fallidos. Espera 15 minutos antes de volver a intentarlo.");
        }

        Optional<UserAccount> found = cedula.isEmpty() ? Optional.empty() : accounts.findByCedula(cedula);
        boolean valid = found.filter(UserAccount::hasPassword)
                .map(a -> password != null && passwordHasher.matches(password, a.getPasswordHash()))
                .orElse(false);
        if (!valid) {
            auditLog.record(AuditAction.LOGIN, null, found.map(UserAccount::getId).orElse(null), cedula,
                    AuditEvent.Outcome.FAILURE, null);
            throw ApplicationException.unauthenticated(INVALID_CREDENTIALS);
        }

        UserAccount account = found.get();
        if (account.getStatus() == AccountStatus.PENDING_APPROVAL) {
            auditLog.record(AuditAction.LOGIN, account.getId(), account.getId(), cedula, AuditEvent.Outcome.DENIED,
                    "pendiente de aprobación");
            throw ApplicationException.forbidden(
                    "Tu solicitud de cuenta docente está pendiente de aprobación por un administrador.");
        }
        if (account.getStatus() == AccountStatus.DISABLED) {
            auditLog.record(AuditAction.LOGIN, account.getId(), account.getId(), cedula, AuditEvent.Outcome.DENIED,
                    "cuenta desactivada");
            throw ApplicationException.forbidden("Tu cuenta está desactivada. Comunícate con un administrador.");
        }
        auditLog.record(AuditAction.LOGIN, account.getId(), account.getId(), cedula, AuditEvent.Outcome.SUCCESS, null);
        return Actor.of(account);
    }
}
