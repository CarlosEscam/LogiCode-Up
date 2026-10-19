package co.edu.unipamplona.logicode.identity.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.identity.domain.Cedula;
import co.edu.unipamplona.logicode.identity.domain.Permission;
import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-05, RN-01: el docente habilita cédulas de estudiantes, de forma individual o con un archivo CSV.
 * <p>
 * Formato CSV: una fila por estudiante con {@code cedula} y, opcionalmente, {@code nombre}, separados por coma o
 * punto y coma. La primera fila puede ser un encabezado. La carga desde Excel queda pendiente (decisión P-07).
 */
@Service
public class EnableStudents {

    public static final int MAX_ROWS = 2000;

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
    public ImportResult importCsv(Actor actor, InputStream csv) {
        actor.require(Permission.MANAGE_STUDENT_ENABLEMENTS);
        int enabled = 0;
        int already = 0;
        List<RowError> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("﻿")) {
                    line = line.substring(1);
                }
                if (line.isBlank()) {
                    continue;
                }
                if (lineNumber > MAX_ROWS + 1) {
                    throw ApplicationException.businessRule("El archivo supera el máximo de " + MAX_ROWS + " filas.");
                }
                String[] columns = line.split("[;,]", -1);
                String rawCedula = columns[0].trim().replace("\"", "");
                String name = columns.length > 1 ? columns[1].trim().replace("\"", "") : null;
                String cedula = Cedula.normalize(rawCedula);
                if (cedula == null) {
                    if (lineNumber == 1) {
                        continue; // encabezado
                    }
                    errors.add(new RowError(lineNumber, rawCedula, "La cédula debe contener solo números."));
                    continue;
                }
                if (!seen.add(cedula) || enablements.existsByCedula(cedula)) {
                    already++;
                    continue;
                }
                enablements.save(new StudentEnablement(cedula, cleanName(name), actor.userId(), clock.instant()));
                enabled++;
            }
        } catch (IOException e) {
            throw ApplicationException.businessRule("No fue posible leer el archivo CSV.");
        }
        auditLog.record(AuditAction.STUDENTS_ENABLED, actor.userId(), null, null, AuditEvent.Outcome.SUCCESS,
                String.valueOf(enabled));
        return new ImportResult(enabled, already, errors);
    }

    @Transactional(readOnly = true)
    public PageResult<StudentEnablement> list(Actor actor, int page, int size) {
        actor.require(Permission.MANAGE_STUDENT_ENABLEMENTS);
        return enablements.list(Math.max(page, 0), Math.clamp(size, 1, 100));
    }

    private static String cleanName(String name) {
        if (name == null) {
            return null;
        }
        String cleaned = name.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned.substring(0, Math.min(cleaned.length(), 150));
    }
}
