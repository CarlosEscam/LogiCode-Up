package co.edu.unipamplona.logicode.identity.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.edu.unipamplona.logicode.identity.domain.Cedula;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;

/**
 * Lee la lista de estudiantes en CSV: una fila por estudiante con {@code cedula} y, opcionalmente, {@code nombre},
 * separados por coma o punto y coma. La primera fila puede ser un encabezado. Las cédulas repetidas se cuentan una
 * vez. La carga desde Excel queda pendiente (decisión P-07).
 */
public final class StudentCsvReader {

    public static final int MAX_ROWS = 2000;

    public record Row(int line, String cedula, String fullName) {
    }

    public record RowError(int line, String value, String reason) {
    }

    public record Result(List<Row> rows, int duplicates, List<RowError> errors) {
    }

    private StudentCsvReader() {
    }

    public static Result read(InputStream csv) {
        Map<String, Row> rows = new LinkedHashMap<>();
        List<RowError> errors = new ArrayList<>();
        int duplicates = 0;
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
                if (rows.putIfAbsent(cedula, new Row(lineNumber, cedula, name)) != null) {
                    duplicates++;
                }
            }
        } catch (IOException e) {
            throw ApplicationException.businessRule("No fue posible leer el archivo CSV.");
        }
        return new Result(List.copyOf(rows.values()), duplicates, errors);
    }
}
