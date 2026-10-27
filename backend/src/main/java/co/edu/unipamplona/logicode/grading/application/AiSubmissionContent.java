package co.edu.unipamplona.logicode.grading.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.files.application.StoredFileInfo;
import org.springframework.stereotype.Component;

/**
 * Qué puede leer la IA y cómo se le entrega. Lee respuestas escritas, fotos PNG/JPG, archivos de texto de PSeInt y
 * Arduino y PDF de hasta {@value PdfContent#MAX_PAGES} páginas (texto y páginas como imágenes, P-08). Word,
 * PowerPoint, videos y Scratch pasan al docente. Si una entrega trae algún archivo que la IA no lee, no se califica a
 * medias: va completa al docente.
 */
@Component
class AiSubmissionContent {

    static final Set<String> IMAGE_FORMATS = Set.of("PNG", "JPG");
    static final Set<String> TEXT_FORMATS = Set.of("PSC", "INO");
    /** Fotos más grandes no se cargan en memoria para el modelo. */
    static final long MAX_IMAGE_BYTES = 25L * 1024 * 1024;
    static final long MAX_TEXT_BYTES = 256L * 1024;
    static final long MAX_PDF_BYTES = 25L * 1024 * 1024;
    /** Imágenes por entrega (fotos más páginas de PDF); en CPU cada una tarda minutos. */
    static final int MAX_IMAGES = 10;

    /** Contenido listo para el proveedor. */
    record Loaded(String textAnswer, List<GradingProvider.TextFile> textFiles, List<GradingProvider.Image> images) {
    }

    private final ActivityCatalog catalog;
    private final FileStore files;

    AiSubmissionContent(ActivityCatalog catalog, FileStore files) {
        this.catalog = catalog;
        this.files = files;
    }

    boolean isReadable(UUID submissionId) {
        return catalog.content(submissionId).map(content -> readable(files.describe(content.fileIds()), content))
                .orElse(false);
    }

    /** Código PSeInt de la entrega: el primer archivo {@code .psc} o, si no hay, la respuesta escrita. */
    Optional<String> pseintCode(UUID submissionId) {
        Optional<ActivityCatalog.SubmissionContent> found = catalog.content(submissionId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Map<UUID, StoredFileInfo> described = files.describe(found.get().fileIds());
        for (UUID fileId : found.get().fileIds()) {
            StoredFileInfo info = described.get(fileId);
            if (info != null && info.format().equals("PSC") && info.sizeBytes() <= MAX_TEXT_BYTES) {
                return Optional.of(decode(read(fileId, info.sizeBytes())));
            }
        }
        return Optional.ofNullable(found.get().textAnswer());
    }

    /** Respuesta escrita de la entrega (el programa de un reto de bloques). */
    Optional<String> textAnswer(UUID submissionId) {
        return catalog.content(submissionId).map(ActivityCatalog.SubmissionContent::textAnswer);
    }

    /** Carga el contenido; vacío si la entrega ya no existe o trae algo que la IA no lee. */
    Optional<Loaded> load(UUID submissionId) {
        Optional<ActivityCatalog.SubmissionContent> found = catalog.content(submissionId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ActivityCatalog.SubmissionContent content = found.get();
        Map<UUID, StoredFileInfo> described = files.describe(content.fileIds());
        if (!readable(described, content)) {
            return Optional.empty();
        }
        List<GradingProvider.TextFile> texts = new ArrayList<>();
        List<GradingProvider.Image> images = new ArrayList<>();
        int position = 0;
        for (UUID fileId : content.fileIds()) {
            position++;
            StoredFileInfo info = described.get(fileId);
            byte[] bytes = read(fileId, info.sizeBytes());
            if (IMAGE_FORMATS.contains(info.format())) {
                images.add(new GradingProvider.Image("Imagen " + (images.size() + 1), bytes));
            } else if (info.format().equals("PDF")) {
                Optional<PdfContent.Pages> pdf = PdfContent.read(bytes);
                if (pdf.isEmpty()) {
                    return Optional.empty();
                }
                if (!pdf.get().text().isEmpty()) {
                    texts.add(new GradingProvider.TextFile("Archivo " + position, "PDF, texto extraído",
                            pdf.get().text()));
                }
                pdf.get().images().forEach(page -> images.add(
                        new GradingProvider.Image("Imagen " + (images.size() + 1), page)));
            } else {
                texts.add(new GradingProvider.TextFile("Archivo " + position, info.format(), decode(bytes)));
            }
        }
        if (images.size() > MAX_IMAGES) {
            return Optional.empty();
        }
        return Optional.of(new Loaded(content.textAnswer(), texts, images));
    }

    private static boolean readable(Map<UUID, StoredFileInfo> described, ActivityCatalog.SubmissionContent content) {
        return described.size() == content.fileIds().size() && described.values().stream().allMatch(f -> IMAGE_FORMATS.contains(f.format()) && f.sizeBytes() <= MAX_IMAGE_BYTES
                || TEXT_FORMATS.contains(f.format()) && f.sizeBytes() <= MAX_TEXT_BYTES
                || f.format().equals("PDF") && f.sizeBytes() <= MAX_PDF_BYTES);
    }

    private byte[] read(UUID fileId, long size) {
        try (InputStream in = files.open(fileId).resource().getInputStream()) {
            return in.readNBytes((int) size);
        } catch (IOException e) {
            throw new UncheckedIOException("No fue posible leer un archivo de la entrega", e);
        }
    }

    /** PSeInt en Windows suele guardar en Windows-1252; se intenta UTF-8 primero. */
    static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }
}
