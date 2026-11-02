package co.edu.unipamplona.logicode.files.application;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.files.domain.StoredFile;

public interface StoredFileRepository {

    StoredFile save(StoredFile file);

    Optional<StoredFile> findById(UUID id);

    List<StoredFile> findAllById(Collection<UUID> ids);

    /** Archivos sin borrar cuya conservación terminó antes de {@code now}. */
    List<StoredFile> findExpired(Instant now);
}
