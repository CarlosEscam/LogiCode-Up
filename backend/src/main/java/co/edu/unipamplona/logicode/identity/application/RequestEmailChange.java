package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.Cedula;
import co.edu.unipamplona.logicode.identity.domain.EmailAddress;
import co.edu.unipamplona.logicode.identity.domain.RecoveryKind;
import co.edu.unipamplona.logicode.identity.domain.RecoveryRequest;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-30, RN-25: cambio asistido de correo cuando se perdió el acceso al vinculado.
 * <p>
 * Compara el último correo registrado y verifica la contraseña contra el hash actual o el anterior. Si ambos
 * coinciden, envía un enlace de confirmación al correo nuevo; el correo nuevo reemplaza al anterior solo al
 * confirmarse. La respuesta es siempre la misma. Límite: {@value RecoveryRequest#DAILY_LIMIT} intentos por cuenta y
 * día calendario en Colombia, contados aparte de la recuperación de contraseña (D-04). La contraseña ingresada
 * nunca se guarda.
 */
@Service
public class RequestEmailChange {

    public record Command(String cedula, String lastEmail, String password, String newEmail) {

        @Override
        public String toString() {
            return "Command[cedula=" + cedula + "]";
        }
    }

    private final UserAccountRepository accounts;
    private final RecoveryRequestRepository recoveryRequests;
    private final PasswordHasher passwordHasher;
    private final EmailVerificationIssuer verificationIssuer;
    private final AuditLog auditLog;
    private final Clock clock;
    /** Hash de relleno: una cédula inexistente tarda lo mismo que una real (no se distinguen por el tiempo). */
    private final String unknownAccountHash;

    public RequestEmailChange(UserAccountRepository accounts, RecoveryRequestRepository recoveryRequests,
            PasswordHasher passwordHasher, EmailVerificationIssuer verificationIssuer, AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
        this.recoveryRequests = recoveryRequests;
        this.passwordHasher = passwordHasher;
        this.verificationIssuer = verificationIssuer;
        this.auditLog = auditLog;
        this.clock = clock;
        this.unknownAccountHash = passwordHasher.hash("cuenta-inexistente#2026");
    }

    @Transactional
    public void execute(Command command) {
        String cedula = Cedula.normalize(command.cedula());
        String lastEmail = EmailAddress.normalize(command.lastEmail());
        String newEmail = EmailAddress.normalize(command.newEmail());
        Optional<UserAccount> found = cedula == null ? Optional.empty()
                : accounts.findByCedula(cedula).filter(a -> a.getStatus() != AccountStatus.DISABLED);
        if (found.isEmpty()) {
            matches(String.valueOf(command.password()), unknownAccountHash);
            auditLog.record(AuditAction.EMAIL_CHANGE_REQUESTED, null, null, cedula, AuditEvent.Outcome.FAILURE,
                    "cuenta inexistente o desactivada");
            return;
        }

        UserAccount account = found.get();
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        if (!RecoveryRequest.withinDailyLimit(
                recoveryRequests.countCountedFor(account.getId(), RecoveryKind.EMAIL_CHANGE, today))) {
            recoveryRequests.save(new RecoveryRequest(account.getId(), RecoveryKind.EMAIL_CHANGE, today,
                    RecoveryRequest.Outcome.LIMIT_REACHED, now));
            auditLog.record(AuditAction.EMAIL_CHANGE_REQUESTED, null, account.getId(), cedula,
                    AuditEvent.Outcome.LIMIT_REACHED, null);
            return;
        }

        boolean emailMatches = lastEmail != null && lastEmail.equalsIgnoreCase(account.getEmail());
        boolean passwordMatches = command.password() != null
                && (matches(command.password(), account.getPasswordHash())
                        || matches(command.password(), account.getPreviousPasswordHash()));
        boolean newEmailAvailable = newEmail != null && !newEmail.equalsIgnoreCase(account.getEmail())
                && !accounts.existsByEmailIgnoreCase(newEmail);

        if (!emailMatches || !passwordMatches || !newEmailAvailable) {
            recoveryRequests.save(new RecoveryRequest(account.getId(), RecoveryKind.EMAIL_CHANGE, today,
                    RecoveryRequest.Outcome.REJECTED, now));
            String reason = !emailMatches || !passwordMatches ? "datos no coinciden" : "correo nuevo no disponible";
            auditLog.record(AuditAction.EMAIL_CHANGE_REQUESTED, null, account.getId(), cedula,
                    AuditEvent.Outcome.FAILURE, reason);
            return;
        }

        recoveryRequests.save(new RecoveryRequest(account.getId(), RecoveryKind.EMAIL_CHANGE, today,
                RecoveryRequest.Outcome.ACCEPTED, now));
        verificationIssuer.issue(account, newEmail);
        auditLog.record(AuditAction.EMAIL_CHANGE_REQUESTED, null, account.getId(), cedula, AuditEvent.Outcome.SUCCESS,
                null);
    }

    private boolean matches(String raw, String hash) {
        return hash != null && passwordHasher.matches(raw, hash);
    }
}
