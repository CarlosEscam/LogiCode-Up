package co.edu.unipamplona.logicode.content.application;

import java.time.Instant;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.domain.MaterialKind;
import co.edu.unipamplona.logicode.content.domain.Tool;
import co.edu.unipamplona.logicode.content.domain.Visibility;
import co.edu.unipamplona.logicode.files.application.StoredFileInfo;

/**
 * Material tal como lo ve un actor. {@code file} es nulo para un enlace; {@code canEdit} indica si puede editarlo
 * (docente del curso) y {@code canModerate} si puede cambiar su visibilidad o archivarlo (además, Administrador).
 */
public record MaterialView(UUID id, UUID topicId, String topicTitle, Tool tool, UUID courseId, String courseName,
        String title, String description, MaterialKind kind, Visibility visibility, int position, String externalUrl,
        StoredFileInfo file, Instant createdAt, Instant updatedAt, boolean canEdit, boolean canModerate) {
}
