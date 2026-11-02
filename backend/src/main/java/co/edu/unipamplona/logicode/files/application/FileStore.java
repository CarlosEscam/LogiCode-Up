package co.edu.unipamplona.logicode.files.application;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.files.domain.FileFormat;
import co.edu.unipamplona.logicode.files.domain.StoredFile;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Recibe, valida y entrega archivos (RNF-04, RN-21). Los demás módulos guardan solo el {@code id} del archivo.
 * <p>
 * La autorización de quién puede subir o ver un archivo la decide el módulo dueño del recurso antes de llamar aquí.
 */
@Service
public class FileStore {

    private static final Logger log = LoggerFactory.getLogger(FileStore.class);

    public record FileContent(StoredFileInfo info, Resource resource) {
    }

    private final BinaryStorage storage;
    private final StoredFileRepository files;
    private final FilesSettings settings;
    private final Clock clock;

    public FileStore(BinaryStorage storage, StoredFileRepository files, FilesSettings settings, Clock clock) {
        this.storage = storage;
        this.files = files;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * Guarda un archivo si su extensión está entre los formatos {@code accepted}, su contenido corresponde a esa extensión y no
     * supera el límite (500 MB; 1 GB para videos). Debe llamarse dentro de una transacción: si la transacción se
     * revierte, el archivo guardado se borra.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StoredFileInfo store(InputStream content, String originalName, UUID uploadedBy, Set<AcceptedFiles> accepted) {
        String name = cleanName(originalName);
        Set<FileFormat> allowed = accepted.stream().flatMap(a -> a.formats().stream())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileFormat.class)));
        FileFormat format = FileFormat.fromFilename(name)
                .filter(allowed::contains)
                .orElseThrow(() -> ApplicationException.businessRule("Formato no permitido. Se aceptan: "
                        + allowed.stream().flatMap(f -> f.extensions().stream()).sorted()
                                .collect(Collectors.joining(", ")) + "."));
        BinaryStorage.StagedFile staged;
        try {
            staged = storage.stage(content, format.maxBytes());
        } catch (BinaryStorage.TooLargeException e) {
            throw ApplicationException.businessRule(format.isVideo()
                    ? "El video supera el máximo de 1 GB." : "El archivo supera el máximo de 500 MB.");
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible recibir el archivo", e);
        }
        try {
            if (staged.size() == 0) {
                throw ApplicationException.businessRule("El archivo está vacío.");
            }
            Set<String> entries = format.isZipContainer() ? storage.zipEntryNames(staged) : Set.of();
            if (!format.matchesContent(staged.head(), entries)) {
                throw ApplicationException.businessRule("El contenido del archivo no corresponde a un archivo ."
                        + format.extensions().getFirst() + " válido.");
            }
            String key = storage.commit(staged);
            deleteOnRollback(key);
            Instant now = clock.instant();
            Instant retainUntil = ZonedDateTime.ofInstant(now, clock.getZone())
                    .plusMonths(settings.retentionMonths()).toInstant();
            StoredFile saved = files.save(new StoredFile(key, name, format, staged.size(), staged.sha256(),
                    uploadedBy, now, retainUntil));
            return StoredFileInfo.of(saved);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible guardar el archivo", e);
        } finally {
            storage.discard(staged);
        }
    }

    @Transactional(readOnly = true)
    public FileContent open(UUID fileId) {
        StoredFile file = files.findById(fileId).orElseThrow(() -> ApplicationException.notFound("El archivo no existe."));
        if (file.isDeleted()) {
            throw ApplicationException.notFound("El archivo se borró al cumplir su tiempo de conservación.");
        }
        return new FileContent(StoredFileInfo.of(file), storage.open(file.getStorageKey()));
    }

    @Transactional(readOnly = true)
    public Map<UUID, StoredFileInfo> describe(Collection<UUID> fileIds) {
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        return files.findAllById(fileIds).stream().map(StoredFileInfo::of)
                .collect(Collectors.toMap(StoredFileInfo::id, Function.identity()));
    }

    private void deleteOnRollback(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    log.info("Se descarta un archivo cargado porque la operación no se completó");
                    storage.delete(key);
                }
            }
        });
    }

    private static String cleanName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw ApplicationException.businessRule("El archivo no tiene nombre.");
        }
        // Solo el nombre, sin rutas del equipo del usuario; sin caracteres de control.
        String name = originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            throw ApplicationException.businessRule("El archivo no tiene nombre.");
        }
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }
}
