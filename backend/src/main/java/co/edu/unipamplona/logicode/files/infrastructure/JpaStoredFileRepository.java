package co.edu.unipamplona.logicode.files.infrastructure;

import java.util.UUID;

import co.edu.unipamplona.logicode.files.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaStoredFileRepository extends JpaRepository<StoredFile, UUID> {
}
