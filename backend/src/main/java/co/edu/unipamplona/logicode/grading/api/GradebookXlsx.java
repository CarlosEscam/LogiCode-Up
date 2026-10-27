package co.edu.unipamplona.logicode.grading.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;

import co.edu.unipamplona.logicode.grading.application.Gradebook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Planilla en Excel .xlsx (RF-18, P-07): las mismas filas que el CSV, con las notas y los porcentajes como números
 * (Excel los muestra con la coma decimal del equipo). Los textos van como texto, así que nunca se ejecutan como fórmula.
 */
final class GradebookXlsx {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private GradebookXlsx() {
    }

    static byte[] write(Gradebook.View view, Clock clock) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Planilla");
            Font boldFont = book.createFont();
            boldFont.setBold(true);
            CellStyle bold = book.createCellStyle();
            bold.setFont(boldFont);
            CellStyle score = book.createCellStyle();
            score.setDataFormat(book.createDataFormat().getFormat("0.0"));
            // Formato General: muestra 30 o 33,33 sin un punto colgando.
            CellStyle percent = book.createCellStyle();
            percent.setFont(boldFont);

            int r = 0;
            Row title = sheet.createRow(r++);
            text(title, 0, "Planilla").setCellStyle(bold);
            text(title, 1, view.courseName()).setCellStyle(bold);
            text(title, 2, view.period());
            Row generated = sheet.createRow(r++);
            text(generated, 0, "Generada");
            text(generated, 1, WHEN.format(clock.instant().atZone(clock.getZone())));
            text(generated, 2, "Escala 0,0 a 5,0");
            text(generated, 3, "Aprueba con " + GradebookCsv.number(view.passingScore()));
            if (view.weightsWarning() != null) {
                Row warning = sheet.createRow(r++);
                text(warning, 0, "Aviso");
                text(warning, 1, view.weightsWarning());
            }
            r++;

            int columns = view.columns().size();
            Row header = sheet.createRow(r++);
            text(header, 0, "Cédula");
            text(header, 1, "Estudiante");
            for (int i = 0; i < columns; i++) {
                text(header, 2 + i, view.columns().get(i).title());
            }
            text(header, 2 + columns, "Avance parcial");
            text(header, 3 + columns, "Nota definitiva");
            header.forEach(cell -> cell.setCellStyle(bold));

            Row weights = sheet.createRow(r++);
            text(weights, 1, "Porcentaje (%)").setCellStyle(bold);
            for (int i = 0; i < columns; i++) {
                number(weights, 2 + i, view.columns().get(i).effectiveWeight(), percent);
            }
            sheet.createFreezePane(2, r);

            for (Gradebook.Row row : view.rows()) {
                Row line = sheet.createRow(r++);
                text(line, 0, row.cedula());
                text(line, 1, row.fullName() == null ? "" : row.fullName());
                for (int i = 0; i < row.cells().size(); i++) {
                    Gradebook.Cell cell = row.cells().get(i);
                    switch (cell.status()) {
                        case GRADED, OVERDUE -> number(line, 2 + i, cell.score(), score);
                        case IN_REVIEW -> text(line, 2 + i, "En revisión");
                        case PENDING -> { }
                    }
                }
                number(line, 2 + columns, row.partial(), score);
                number(line, 3 + columns, row.definitive(), score);
            }

            sheet.setColumnWidth(0, 14 * 256);
            sheet.setColumnWidth(1, 32 * 256);
            for (int i = 2; i < 4 + columns; i++) {
                sheet.setColumnWidth(i, 16 * 256);
            }
            book.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No fue posible generar la planilla en Excel", e);
        }
    }

    private static org.apache.poi.ss.usermodel.Cell text(Row row, int column, String value) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(column);
        cell.setCellValue(value);
        return cell;
    }

    private static void number(Row row, int column, BigDecimal value, CellStyle style) {
        if (value != null) {
            org.apache.poi.ss.usermodel.Cell cell = row.createCell(column);
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        }
    }
}
