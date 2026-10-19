package co.edu.unipamplona.logicode.files.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FileFormatTest {

    @Test
    void formatComesFromTheExtensionIgnoringCase() {
        assertThat(FileFormat.fromFilename("Guía.PDF")).contains(FileFormat.PDF);
        assertThat(FileFormat.fromFilename("foto.jpeg")).contains(FileFormat.JPG);
        assertThat(FileFormat.fromFilename("proyecto.sb3")).contains(FileFormat.SB3);
        assertThat(FileFormat.fromFilename("programa.exe")).isEmpty();
        assertThat(FileFormat.fromFilename("sin-extension")).isEmpty();
        assertThat(FileFormat.fromFilename("termina-en-punto.")).isEmpty();
    }

    @Test
    void contentMustMatchTheExtension() {
        byte[] pdf = "%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
        byte[] zip = {'P', 'K', 0x03, 0x04, 0};
        assertThat(FileFormat.PDF.matchesContent(pdf, Set.of())).isTrue();
        assertThat(FileFormat.PDF.matchesContent(png, Set.of())).isFalse();
        assertThat(FileFormat.PNG.matchesContent(png, Set.of())).isTrue();
        assertThat(FileFormat.JPG.matchesContent(png, Set.of())).isFalse();
        assertThat(FileFormat.DOCX.matchesContent(zip, Set.of("word/document.xml"))).isTrue();
        assertThat(FileFormat.DOCX.matchesContent(zip, Set.of("ppt/presentation.xml"))).isFalse();
        assertThat(FileFormat.SB3.matchesContent(zip, Set.of("project.json"))).isTrue();
        assertThat(FileFormat.PSC.matchesContent("Algoritmo hola\nFinAlgoritmo".getBytes(), Set.of())).isTrue();
        assertThat(FileFormat.INO.matchesContent(new byte[] {'M', 'Z', 0, 0}, Set.of())).isFalse();
    }

    @Test
    void videosAllowOneGigabyteAndTheRestFiveHundredMegabytes() {
        assertThat(FileFormat.MP4.maxBytes()).isEqualTo(1024L * 1024 * 1024);
        assertThat(FileFormat.PDF.maxBytes()).isEqualTo(500L * 1024 * 1024);
        assertThat(FileFormat.PDF.isPreviewable()).isTrue();
        assertThat(FileFormat.DOCX.isPreviewable()).isFalse();
    }
}
