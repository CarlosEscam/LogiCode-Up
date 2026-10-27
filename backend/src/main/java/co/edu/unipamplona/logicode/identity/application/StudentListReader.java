package co.edu.unipamplona.logicode.identity.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.edu.unipamplona.logicode.identity.domain.Cedula;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Lee la lista de estudiantes desde CSV o Excel (.xlsx, primera hoja): una fila por estudiante con {@code cedula} y,
 * opcionalmente, {@code nombre} (en CSV separados por coma o punto y coma; en Excel, columnas A y B). La primera fila
 * con datos puede ser un encabezado. Las cédulas repetidas se cuentan una vez.
 */
public final class StudentListReader {

    public static final int MAX_ROWS = 2000;
    /** Una hoja de 2000 filas ocupa unos cientos de KB descomprimida; más que esto es un archivo armado para dañar. */
    private static final long MAX_XLSX_PART_BYTES = 20L * 1024 * 1024;

    public record Row(int line, String cedula, String fullName) {
    }

    public record RowError(int line, String value, String reason) {
    }

    public record Result(List<Row> rows, int duplicates, List<RowError> errors) {
    }

    private StudentListReader() {
    }

    /** Elige el formato por la extensión del archivo (.csv o .xlsx). */
    public static Result read(InputStream in, String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx")) {
            return readXlsx(in);
        }
        if (name.endsWith(".csv")) {
            return readCsv(in);
        }
        throw ApplicationException.businessRule("Sube un archivo .csv o .xlsx con una cédula por fila.");
    }

    static Result readCsv(InputStream csv) {
        Collector collector = new Collector();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("﻿")) {
                    line = line.substring(1);
                }
                String[] columns = line.split("[;,]", -1);
                collector.add(lineNumber, columns[0].replace("\"", ""),
                        columns.length > 1 ? columns[1].replace("\"", "") : null);
            }
        } catch (IOException e) {
            throw ApplicationException.businessRule("No fue posible leer el archivo CSV.");
        }
        return collector.result();
    }

    static Result readXlsx(InputStream xlsx) {
        ZipSecureFile.setMaxEntrySize(MAX_XLSX_PART_BYTES);
        Collector collector = new Collector();
        try (XSSFWorkbook book = new XSSFWorkbook(xlsx)) {
            Sheet sheet = book.getSheetAt(0);
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                collector.add(row.getRowNum() + 1, text(row.getCell(0)), text(row.getCell(1)));
            }
        } catch (ApplicationException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // POI informa un archivo dañado, cifrado o que no es .xlsx con distintas excepciones.
            throw ApplicationException.businessRule(
                    "No fue posible leer el archivo de Excel. Guárdalo como \"Libro de Excel (.xlsx)\" o como CSV.");
        }
        return collector.result();
    }

    /** Texto de la celda; los números salen sin decimales ni notación científica (una cédula escrita como número). */
    private static String text(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case STRING -> cell.getStringCellValue();
            default -> null;
        };
    }

    private static final class Collector {
        private final Map<String, Row> rows = new LinkedHashMap<>();
        private final List<RowError> errors = new ArrayList<>();
        private int duplicates;
        private boolean first = true;

        void add(int line, String rawCedula, String rawName) {
            String raw = rawCedula == null ? "" : rawCedula.trim();
            String name = rawName == null || rawName.isBlank() ? null : rawName.trim();
            if (raw.isEmpty() && name == null) {
                return;
            }
            if (line > MAX_ROWS + 1) {
                throw ApplicationException.businessRule("El archivo supera el máximo de " + MAX_ROWS + " filas.");
            }
            boolean header = first;
            first = false;
            String cedula = Cedula.normalize(raw);
            if (cedula == null) {
                if (!header) {
                    errors.add(new RowError(line, raw, "La cédula debe contener solo números."));
                }
                return;
            }
            if (rows.putIfAbsent(cedula, new Row(line, cedula, name)) != null) {
                duplicates++;
            }
        }

        Result result() {
            return new Result(List.copyOf(rows.values()), duplicates, errors);
        }
    }
}
