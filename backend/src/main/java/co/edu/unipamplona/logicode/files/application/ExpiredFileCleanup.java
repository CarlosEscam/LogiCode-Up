package co.edu.unipamplona.logicode.files.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.files.domain.StoredFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Borra los archivos que ya cumplieron su tiempo de conservación (RN-21, RNF-12) y que ningún módulo usa. El registro
 * queda marcado como borrado; el contenido sale del almacenamiento después de confirmar en la base.
 */
@Service
public class ExpiredFileCleanup {

    private static final int CHUNK = 500;

    private final StoredFileRepository files;
    private final BinaryStorage storage;
    private final List<FileUsage> usages;
    private final Clock clock;

    public ExpiredFileCleanup(StoredFileRepository files, BinaryStorage storage, List<FileUsage> usages, Clock clock) {
        this.files = files;
        this.storage = storage;
        this.usages = usages;
        this.clock = clock;
    }

    /** Devuelve cuántos archivos borró. */
    @Transactional
    public int deleteExpired() {
        Instant now = clock.instant();
        // shortcut: carga todos los vencidos de una vez; si llegan a ser decenas de miles, paginar por id.
        List<StoredFile> expired = files.findExpired(now);
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < expired.size(); i += CHUNK) {
            List<StoredFile> chunk = expired.subList(i, Math.min(i + CHUNK, expired.size()));
            Set<UUID> ids = chunk.stream().map(StoredFile::getId).collect(Collectors.toSet());
            Set<UUID> inUse = usages.stream().flatMap(u -> u.inUse(ids).stream()).collect(Collectors.toSet());
            for (StoredFile file : chunk) {
                if (!inUse.contains(file.getId())) {
                    file.markDeleted(now);
                    keys.add(file.getStorageKey());
                }
            }
        }
        if (!keys.isEmpty()) {
            // Si un borrado falla queda un objeto suelto en el almacenamiento, nunca un registro sin contenido.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    keys.forEach(storage::delete);
                }
            });
        }
        return keys.size();
    }
}
