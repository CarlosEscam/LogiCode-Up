package co.edu.unipamplona.logicode.grading.api;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.grading.application.Gradebook;

/**
 * Planilla en CSV para Excel (RF-18, P-07): separador punto y coma, coma decimal y UTF-8 con BOM, como lo abre Excel
 * en español. Escribe exactamente las cifras de la pantalla (pesos efectivos, notas, parcial y definitiva).
 */
final class GradebookCsv {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private GradebookCsv() {
    }

    static String write(Gradebook.View view, Clock clock) {
        List<List<String>> lines = new ArrayList<>();
        lines.add(List.of("Planilla", view.courseName(), view.period()));
        lines.add(List.of("Generada", WHEN.format(clock.instant().atZone(clock.getZone())), "Escala 0,0 a 5,0",
                "Aprueba con " + number(view.passingScore())));
        if (view.weightsWarning() != null) {
            lines.add(List.of("Aviso", view.weightsWarning()));
        }
        lines.add(List.of());

        List<String> header = new ArrayList<>(List.of("Cédula", "Estudiante"));
        view.columns().forEach(c -> header.add(c.title()));
        header.addAll(List.of("Avance parcial", "Nota definitiva"));
        lines.add(header);

        List<String> weights = new ArrayList<>(List.of("", "Porcentaje"));
        view.columns().forEach(c -> weights.add(number(c.effectiveWeight().stripTrailingZeros()) + " %"));
        lines.add(weights);

        for (Gradebook.Row row : view.rows()) {
            List<String> line = new ArrayList<>(List.of(row.cedula(), row.fullName() == null ? "" : row.fullName()));
            row.cells().forEach(cell -> line.add(cell(cell)));
            line.add(number(row.partial()));
            line.add(number(row.definitive()));
            lines.add(line);
        }
        return "﻿" + lines.stream().map(l -> l.stream().map(GradebookCsv::field)
                .collect(Collectors.joining(";"))).collect(Collectors.joining("\r\n")) + "\r\n";
    }

    static String cell(Gradebook.Cell cell) {
        return switch (cell.status()) {
            case GRADED, OVERDUE -> number(cell.score());
            case IN_REVIEW -> "En revisión";
            case PENDING -> "";
        };
    }

    static String number(BigDecimal value) {
        return value == null ? "" : value.toPlainString().replace('.', ',');
    }

    /** Comillas si hace falta y apóstrofo ante =, +, - o @ para que Excel no lo ejecute como fórmula. */
    static String field(String value) {
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return safe.contains(";") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")
                ? "\"" + safe.replace("\"", "\"\"") + "\"" : safe;
    }
}
