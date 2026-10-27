package co.edu.unipamplona.logicode.grading.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfContentTest {

    @Test
    void readsTextAndRendersEachPage() throws IOException {
        PdfContent.Pages pages = PdfContent.read(pdf(2, PDRectangle.LETTER, "Escribir prom")).orElseThrow();
        assertThat(pages.text()).contains("Escribir prom");
        assertThat(pages.images()).hasSize(2)
                .allSatisfy(png -> assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G'));
    }

    @Test
    void leavesLongOrDamagedFilesToTheTeacher() throws IOException {
        assertThat(PdfContent.read(pdf(PdfContent.MAX_PAGES + 1, PDRectangle.LETTER, "x"))).isEmpty();
        assertThat(PdfContent.read("%PDF-1.4 roto".getBytes())).isEmpty();
        // Un escaneo no trae texto: solo van las imágenes.
        assertThat(PdfContent.read(pdf(1, PDRectangle.A4, null))).get()
                .satisfies(pages -> assertThat(pages.text()).isEmpty());
    }

    @Test
    void aHugePageIsRenderedSmall() throws IOException {
        // Una página de 5 × 5 metros a 110 ppp tendría unos 21 000 píxeles por lado.
        PdfContent.Pages pages = PdfContent.read(pdf(1, new PDRectangle(14_000, 14_000), null)).orElseThrow();
        assertThat(pages.images().getFirst().length).isLessThan(2_000_000);
    }

    private static byte[] pdf(int pages, PDRectangle size, String line) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage(size);
                document.addPage(page);
                if (line != null) {
                    try (PDPageContentStream text = new PDPageContentStream(document, page)) {
                        text.beginText();
                        text.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        text.newLineAtOffset(72, 700);
                        text.showText(line);
                        text.endText();
                    }
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
