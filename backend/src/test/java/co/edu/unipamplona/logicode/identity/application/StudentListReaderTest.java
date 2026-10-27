package co.edu.unipamplona.logicode.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class StudentListReaderTest {

    @Test
    void readsCsvWithHeaderDuplicatesAndBadRows() {
        StudentListReader.Result result = read("cedula;nombre\n1000000201;Ana\n\n1000000201;Otra\nabc;Mala\n", "a.csv");
        assertThat(result.rows()).extracting(StudentListReader.Row::cedula).containsExactly("1000000201");
        assertThat(result.rows().getFirst().fullName()).isEqualTo("Ana");
        assertThat(result.duplicates()).isEqualTo(1);
        assertThat(result.errors()).singleElement().satisfies(e -> {
            assertThat(e.line()).isEqualTo(5);
            assertThat(e.value()).isEqualTo("abc");
        });
    }

    @Test
    void readsTheFirstSheetOfAnExcelFile() throws IOException {
        byte[] xlsx;
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Lista");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Cédula");
            header.createCell(1).setCellValue("Nombre");
            // Una cédula escrita como número: Excel la guarda como 1.000000202E9.
            Row number = sheet.createRow(1);
            number.createCell(0).setCellValue(1000000202d);
            number.createCell(1).setCellValue("Bruno Díaz");
            Row text = sheet.createRow(2);
            text.createCell(0).setCellValue("1.000.000.203");
            sheet.createRow(4).createCell(0).setCellValue(123456789012d);
            sheet.createRow(5).createCell(0).setCellValue("sin cédula");
            sheet.createRow(6).createCell(0).setCellValue(1000000202d);
            book.write(out);
            xlsx = out.toByteArray();
        }
        StudentListReader.Result result = StudentListReader.read(new ByteArrayInputStream(xlsx), "Lista.XLSX");
        assertThat(result.rows()).extracting(StudentListReader.Row::cedula)
                .containsExactly("1000000202", "1000000203", "123456789012");
        assertThat(result.rows().getFirst().fullName()).isEqualTo("Bruno Díaz");
        assertThat(result.rows().get(1).fullName()).isNull();
        assertThat(result.duplicates()).isEqualTo(1);
        assertThat(result.errors()).singleElement().satisfies(e -> assertThat(e.line()).isEqualTo(6));
    }

    @Test
    void rejectsOtherFilesWithAClearMessage() {
        assertThatThrownBy(() -> read("PK no es excel", "lista.xlsx")).isInstanceOf(ApplicationException.class)
                .hasMessageContaining("No fue posible leer el archivo de Excel");
        assertThatThrownBy(() -> read("1000000201", "lista.xls")).isInstanceOf(ApplicationException.class)
                .hasMessageContaining(".csv o .xlsx");
    }

    private static StudentListReader.Result read(String content, String name) {
        return StudentListReader.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), name);
    }
}
