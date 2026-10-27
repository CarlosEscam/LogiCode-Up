package co.edu.unipamplona.logicode.grading.application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Prepara un PDF entregado para la IA (P-08, Apache PDFBox): el texto que traiga (un PDF exportado desde Word o
 * PSeInt) y cada página como imagen (un escaneo o una foto convertida a PDF). Un PDF dañado, con contraseña o con más
 * de {@link #MAX_PAGES} páginas no se lee: la entrega completa va al docente.
 */
final class PdfContent {

    static final int MAX_PAGES = 5;
    static final int MAX_TEXT_CHARS = 20_000;
    private static final float DPI = 110;
    /** Lado mayor de cada página en píxeles; evita que una página gigante agote la memoria. */
    private static final int MAX_SIDE_PX = 2000;

    record Pages(String text, List<byte[]> images) {
    }

    private PdfContent() {
    }

    static Optional<Pages> read(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            int pages = document.getNumberOfPages();
            if (pages == 0 || pages > MAX_PAGES) {
                return Optional.empty();
            }
            String text = new PDFTextStripper().getText(document).strip();
            if (text.length() > MAX_TEXT_CHARS) {
                text = text.substring(0, MAX_TEXT_CHARS);
            }
            PDFRenderer renderer = new PDFRenderer(document);
            List<byte[]> images = new ArrayList<>();
            for (int i = 0; i < pages; i++) {
                PDRectangle box = document.getPage(i).getCropBox();
                float longSide = Math.max(box.getWidth(), box.getHeight());
                float dpi = Math.min(DPI, MAX_SIDE_PX * 72f / Math.max(longSide, 1));
                BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                images.add(out.toByteArray());
            }
            return Optional.of(new Pages(text, images));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }
}
