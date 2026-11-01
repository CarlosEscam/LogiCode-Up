package co.edu.unipamplona.logicode.files.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import co.edu.unipamplona.logicode.files.application.BinaryStorage;
import co.edu.unipamplona.logicode.files.domain.FileFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Guarda los archivos en una carpeta local (volumen {@code file-storage} en Docker). Cada archivo usa una clave
 * aleatoria repartida en subcarpetas ({@code ab/cd/<uuid>}); el nombre original solo vive en la base de datos.
 */
class LocalBinaryStorage implements BinaryStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalBinaryStorage.class);
    private static final int MAX_ZIP_ENTRIES = 10_000;

    private final Path root;
    private final Path staging;

    LocalBinaryStorage(Path root) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        this.staging = this.root.resolve(".staging");
        Files.createDirectories(staging);
    }

    @Override
    public StagedFile stage(InputStream content, long maxBytes) throws IOException {
        Path temp = Files.createTempFile(staging, "upload-", ".part");
        MessageDigest digest = sha256();
        byte[] head = new byte[FileFormat.HEAD_BYTES];
        int headLength = 0;
        long total = 0;
        try (OutputStream out = new DigestOutputStream(Files.newOutputStream(temp), digest)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = content.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new TooLargeException();
                }
                if (headLength < head.length) {
                    int copy = Math.min(read, head.length - headLength);
                    System.arraycopy(buffer, 0, head, headLength, copy);
                    headLength += copy;
                }
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
        return new StagedFile(temp, total, HexFormat.of().formatHex(digest.digest()), Arrays.copyOf(head, headLength));
    }

    @Override
    public Set<String> zipEntryNames(StagedFile staged) {
        Set<String> names = new HashSet<>();
        try (ZipFile zip = new ZipFile(((Path) staged.handle()).toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements() && names.size() < MAX_ZIP_ENTRIES) {
                ZipEntry entry = entries.nextElement();
                names.add(entry.getName());
            }
        } catch (IOException e) {
            return Set.of(); // no es un ZIP válido: el formato no coincidirá
        }
        return names;
    }

    @Override
    public String commit(StagedFile staged) throws IOException {
        String key = newKey();
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.move((Path) staged.handle(), target, StandardCopyOption.ATOMIC_MOVE);
        return key;
    }

    @Override
    public void discard(StagedFile staged) {
        try {
            Files.deleteIfExists((Path) staged.handle());
        } catch (IOException e) {
            log.warn("No se pudo borrar un archivo temporal de carga", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            log.warn("No se pudo borrar un archivo descartado", e);
        }
    }

    @Override
    public Resource open(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Falta en el almacenamiento un archivo registrado en la base de datos");
        }
        return new FileSystemResource(path);
    }

    /** Clave aleatoria repartida en subcarpetas; la usan también los objetos de MinIO. */
    static String newKey() {
        String id = UUID.randomUUID().toString();
        return id.substring(0, 2) + "/" + id.substring(2, 4) + "/" + id;
    }

    boolean contains(String storageKey) {
        return Files.isRegularFile(resolve(storageKey));
    }

    Path root() {
        return root;
    }

    Path staging() {
        return staging;
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || path.startsWith(staging)) {
            throw new IllegalArgumentException("Clave de almacenamiento inválida");
        }
        return path;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
