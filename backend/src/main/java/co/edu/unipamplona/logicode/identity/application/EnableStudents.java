package co.edu.unipamplona.logicode.identity.application;

import java.io.InputStream;
import java.time.Clock;
import java.util.List;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.Cedula;
import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import co.edu.unipamplona.logicode.shared.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-05, RN-01: el docente habilita cédulas de estudiantes, de forma individual o con un archivo CSV o Excel.
 * <p>
 * Formato del archivo: ver {@link StudentListReader}.
 */
@Service
public class EnableStudents {

    public record RowError(int line, String value, String reason) {
    }

    public record ImportResult(int enabled, int alreadyEnabled, List<RowError> errors) {
    }

    private final StudentEnablementRepository enablements;
    private final AuditLog auditLog;
    private final Clock clock;

    public EnableStudents(StudentEnablementRepository enablements, AuditLog auditLog, Clock clock) {
        this.enablements = enablements;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public StudentEnablement enableOne(Actor actor, String rawCedula, String fullName) {
        actor.require(Permission.MANAGE_STUDENT_ENABLEMENTS);
        String cedula = Cedula.normalize(rawCedula);
        if (cedula == null) {
            throw ApplicationException.businessRule("La cédula debe contener solo números.");
        }
        if (enablements.existsByCedula(cedula)) {
            throw ApplicationException.conflict("Esta cédula ya está habilitada.");
        }
        StudentEnablement saved = enablements.save(
                new StudentEnablement(cedula, cleanName(fullName), actor.userId(), clock.instant()));
        auditLog.record(AuditAction.STUDENTS_ENABLED, actor.userId(), null, cedula, AuditEvent.Outcome.SUCCESS, "1");
        return saved;
    }

    @Transactional
    public ImportResult importFile(Actor actor, InputStream in, String fileName) {
        actor.require(Permission.MANAGE_STUDENT_ENABLEMENTS);
        StudentListReader.Result parsed = StudentListReader.read(in, fileName);
        int enabled = 0;
        int already = parsed.duplicates();
        for (StudentListReader.Row row : parsed.rows()) {
            if (enablements.existsByCedula(row.cedula())) {
                already++;
                continue;
            }
            enablements.save(new StudentEnablement(row.cedula(), cleanName(row.fullName()), actor.userId(),
                    clock.instant()));
            enabled++;
        }
        auditLog.record(AuditAction.STUDENTS_ENABLED, actor.userId(), null, null, AuditEvent.Outcome.SUCCESS,
                String.valueOf(enabled));
        List<RowError> errors = parsed.errors().stream()
                .map(e -> new RowError(e.line(), e.value(), e.reason())).toList();
        return new ImportResult(enabled, already, errors);
    }

    /**
     * Habilita la cédula si aún no lo está, sin error si ya lo estaba. La usan otros módulos (inscripción a un
     * curso) para que inscribir a un estudiante también le permita registrarse.
     *
     * @return la cédula normalizada
     */
    @Transactional
    public String ensureEnabled(Actor actor, String rawCedula, String fullName) {
        actor.require(Permission.MANAGE_STUDENT_ENABLEMENTS);
        String cedula = Cedula.normalize(rawCedula);
        if (cedula == null) {
            throw ApplicationException.businessRule("La cédula debe contener solo números.");
        }
        if (!enablements.existsByCedula(cedula)) {
            enablements.save(new StudentEnablement(cedula, cleanName(fullName), actor.userId(), clock.instant()));
            auditLog.record(AuditAction.STUDENTS_ENABLED, actor.userId(), null, cedula, AuditEvent.Outcome.SUCCESS,
                    "1");
        }
        return cedula;
    }

    @Transactional(readOnly = true)
    public PageResult<StudentEnablement> list(Actor actor, int page, int size) {
        actor.require(Permission.MANAGE_STUDENT_ENABLEMENTS);
        // Un docente solo ve las cédulas que él habilitó (Ley 1581: los estudiantes de otros cursos no son suyos).
        boolean staff = actor.fullPrivileges() || actor.role() == Role.ADMIN;
        return enablements.list(staff ? null : actor.userId(), Math.max(page, 0), Math.clamp(size, 1, 100));
    }

    private static String cleanName(String name) {
        if (name == null) {
            return null;
        }
        String cleaned = name.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned.substring(0, Math.min(cleaned.length(), 150));
    }
}
