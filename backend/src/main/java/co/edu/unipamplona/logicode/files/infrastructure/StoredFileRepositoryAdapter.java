package co.edu.unipamplona.logicode.files.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.files.application.StoredFileRepository;
import co.edu.unipamplona.logicode.files.domain.StoredFile;
import org.springframework.stereotype.Repository;

@Repository
class StoredFileRepositoryAdapter implements StoredFileRepository {

    private final JpaStoredFileRepository jpa;

    StoredFileRepositoryAdapter(JpaStoredFileRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public StoredFile save(StoredFile file) {
        return jpa.save(file);
    }

    @Override
    public Optional<StoredFile> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<StoredFile> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids);
    }
}
