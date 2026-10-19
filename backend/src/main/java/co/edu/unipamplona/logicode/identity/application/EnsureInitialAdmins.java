package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.Cedula;
import co.edu.unipamplona.logicode.identity.domain.EmailAddress;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RN-22, D-03: al arrancar, garantiza que las cuentas iniciales configuradas existan como Administrador con
 * privilegio completo y que ninguna otra cuenta lo tenga. Una cuenta nueva no tiene contraseña: recibe el enlace de
 * verificación y, ya verificada, define su contraseña con "¿Olvidaste tu contraseña?".
 */
@Service
public class EnsureInitialAdmins {

    private static final Logger log = LoggerFactory.getLogger(EnsureInitialAdmins.class);

    private final UserAccountRepository accounts;
    private final EmailVerificationIssuer verificationIssuer;
    private final AuditLog auditLog;
    private final Clock clock;

    public EnsureInitialAdmins(UserAccountRepository accounts, EmailVerificationIssuer verificationIssuer,
            AuditLog auditLog, Clock clock) {
        this.accounts = accounts;
        this.verificationIssuer = verificationIssuer;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public void execute(List<InitialAdmin> configured) {
        List<InitialAdmin> admins = configured.stream().filter(InitialAdmin::isConfigured).toList();
        if (admins.isEmpty()) {
            log.warn("No hay cuentas iniciales configuradas (INITIAL_ADMIN_*). Nadie tendrá privilegios completos.");
            return;
        }
        Instant now = clock.instant();
        for (InitialAdmin admin : admins) {
            String cedula = Cedula.normalize(admin.cedula());
            String email = EmailAddress.normalize(admin.email());
            String name = admin.fullName() == null ? "" : admin.fullName().trim();
            if (cedula == null || email == null || name.isEmpty()) {
                log.error("Cuenta inicial con datos incompletos o inválidos; se omite. Revisa INITIAL_ADMIN_*.");
                continue;
            }
            accounts.findByCedula(cedula).ifPresentOrElse(existing -> {
                if (!existing.isActiveFullAdmin()) {
                    existing.ensureInitialAdmin(now);
                    accounts.save(existing);
                    auditLog.record(AuditAction.INITIAL_ADMIN_ENSURED, null, existing.getId(), cedula,
                            AuditEvent.Outcome.SUCCESS, "privilegio completo restablecido");
                }
            }, () -> {
                if (accounts.existsByEmailIgnoreCase(email)) {
                    log.error("El correo de una cuenta inicial ya pertenece a otra cuenta; se omite.");
                    return;
                }
                UserAccount created = accounts.save(UserAccount.initialAdmin(cedula, name, email, now));
                verificationIssuer.issue(created, email);
                auditLog.record(AuditAction.INITIAL_ADMIN_ENSURED, null, created.getId(), cedula,
                        AuditEvent.Outcome.SUCCESS, "cuenta creada");
                log.info("Cuenta inicial creada; se envió el enlace de verificación de correo.");
            });
        }

        Set<String> initialCedulas = admins.stream().map(a -> Cedula.normalize(a.cedula()))
                .collect(Collectors.toSet());
        for (UserAccount other : accounts.findAllWithFullPrivileges()) {
            if (!initialCedulas.contains(other.getCedula())) {
                other.revokeFullPrivileges(now);
                accounts.save(other);
                auditLog.record(AuditAction.INITIAL_ADMIN_ENSURED, null, other.getId(), other.getCedula(),
                        AuditEvent.Outcome.SUCCESS, "privilegio completo retirado: no es cuenta inicial");
            }
        }
    }
}
