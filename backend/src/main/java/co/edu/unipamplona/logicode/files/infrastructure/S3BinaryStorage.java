package co.edu.unipamplona.logicode.files.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import co.edu.unipamplona.logicode.files.application.BinaryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

/**
 * Guarda los archivos en MinIO (o cualquier almacenamiento compatible con S3) con la misma clave aleatoria que el
 * almacenamiento local ({@code ab/cd/<uuid>}). La carga se recibe primero en la carpeta local para medirla, calcular
 * su SHA-256 y revisar el formato; luego se sube y se borra la copia temporal.
 * <p>
 * Los archivos que quedaron en la carpeta local antes de usar MinIO se suben con {@link #migrateLocalFiles()}; mientras
 * tanto se siguen entregando desde la carpeta.
 */
class S3BinaryStorage implements BinaryStorage {

    private static final Logger log = LoggerFactory.getLogger(S3BinaryStorage.class);

    private final LocalBinaryStorage local;
    private final S3ObjectClient s3;

    S3BinaryStorage(LocalBinaryStorage local, S3ObjectClient s3) {
        this.local = local;
        this.s3 = s3;
    }

    @Override
    public StagedFile stage(InputStream content, long maxBytes) throws IOException {
        return local.stage(content, maxBytes);
    }

    @Override
    public Set<String> zipEntryNames(StagedFile staged) {
        return local.zipEntryNames(staged);
    }

    @Override
    public String commit(StagedFile staged) throws IOException {
        String key = LocalBinaryStorage.newKey();
        s3.put(key, (Path) staged.handle(), staged.sha256());
        return key; // la copia temporal la borra discard()
    }

    @Override
    public void discard(StagedFile staged) {
        local.discard(staged);
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3.delete(storageKey);
        } catch (IOException e) {
            log.warn("No se pudo borrar un archivo descartado del almacenamiento", e);
        }
    }

    @Override
    public Resource open(String storageKey) {
        if (local.contains(storageKey)) {
            return local.open(storageKey); // aún no se ha migrado
        }
        long size;
        try {
            size = s3.size(storageKey).orElseThrow(() -> new IllegalStateException(
                    "Falta en el almacenamiento un archivo registrado en la base de datos"));
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el archivo", e);
        }
        return new ObjectResource(storageKey, size);
    }

    /**
     * Sube a MinIO los archivos de la carpeta local que aún no están allí y borra cada copia local después de comprobar
     * que el objeto quedó con el mismo tamaño. Se puede repetir sin efecto: lo ya migrado no está en la carpeta.
     */
    void migrateLocalFiles() {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(local.root())) {
            files = walk.filter(Files::isRegularFile).filter(p -> !p.startsWith(local.staging())).toList();
        } catch (IOException e) {
            log.error("No se pudo recorrer la carpeta local de archivos para pasarlos a MinIO", e);
            return;
        }
        if (files.isEmpty()) {
            return;
        }
        log.info("Pasando {} archivos de la carpeta local a MinIO", files.size());
        int moved = 0;
        for (Path file : files) {
            String key = local.root().relativize(file).toString().replace('\\', '/');
            try {
                long size = Files.size(file);
                if (s3.size(key).orElse(-1) != size) {
                    s3.put(key, file, sha256(file));
                }
                if (s3.size(key).orElse(-1) == size) {
                    Files.delete(file);
                    moved++;
                } else {
                    log.error("El archivo {} no quedó completo en MinIO; se conserva la copia local", key);
                }
            } catch (IOException | RuntimeException e) {
                log.error("No se pudo pasar a MinIO el archivo {}; se conserva la copia local", key, e);
            }
        }
        deleteEmptyFolders();
        log.info("Pasados a MinIO {} de {} archivos", moved, files.size());
    }

    private void deleteEmptyFolders() {
        try (Stream<Path> walk = Files.walk(local.root())) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(local.root()) && !p.startsWith(local.staging()) && Files.isDirectory(p))
                    .forEach(p -> p.toFile().delete()); // solo borra las carpetas vacías
        } catch (IOException e) {
            log.warn("No se pudieron quitar las carpetas vacías de la carpeta local", e);
        }
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Objeto de MinIO como {@link Resource}; Spring lo usa para responder descargas completas y por rangos. */
    private final class ObjectResource extends AbstractResource {

        private final String key;
        private final long size;

        ObjectResource(String key, long size) {
            this.key = key;
            this.size = size;
        }

        @Override
        public InputStream getInputStream() {
            return new ObjectStream(key);
        }

        @Override
        public long contentLength() {
            return size;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Archivo " + key;
        }
    }

    /**
     * Abre el objeto en la primera lectura. Un {@code skip} previo (así atiende Spring los Range de los videos) no lee
     * bytes: pide el objeto desde esa posición.
     */
    private final class ObjectStream extends InputStream {

        private final String key;
        private long offset;
        private InputStream in;

        ObjectStream(String key) {
            this.key = key;
        }

        private InputStream in() throws IOException {
            if (in == null) {
                in = s3.get(key, offset);
            }
            return in;
        }

        @Override
        public long skip(long n) throws IOException {
            if (in == null) {
                offset += Math.max(0, n);
                return Math.max(0, n);
            }
            return in.skip(n);
        }

        @Override
        public int read() throws IOException {
            return in().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return in().read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            if (in != null) {
                in.close();
            }
        }
    }
}
