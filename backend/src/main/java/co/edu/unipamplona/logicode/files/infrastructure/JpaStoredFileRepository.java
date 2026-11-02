package co.edu.unipamplona.logicode.files.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.files.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaStoredFileRepository extends JpaRepository<StoredFile, UUID> {

    List<StoredFile> findByRetainUntilBeforeAndDeletedAtIsNull(Instant now);
}
