package co.edu.unipamplona.logicode.files.application;

import java.time.Instant;
import java.util.UUID;

import co.edu.unipamplona.logicode.files.domain.StoredFile;

/** Datos públicos de un archivo cargado ({@code format}: PDF, DOCX, MP4...). No incluye la clave de almacenamiento. */
public record StoredFileInfo(UUID id, String originalName, String format, String contentType, long sizeBytes,
        boolean previewable, Instant uploadedAt, Instant retainUntil) {

    static StoredFileInfo of(StoredFile file) {
        return new StoredFileInfo(file.getId(), file.getOriginalName(), file.getFormat().name(), file.getContentType(),
                file.getSizeBytes(), file.getFormat().isPreviewable(), file.getUploadedAt(), file.getRetainUntil());
    }
}
