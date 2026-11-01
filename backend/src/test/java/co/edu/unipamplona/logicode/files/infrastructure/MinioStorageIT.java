package co.edu.unipamplona.logicode.files.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import co.edu.unipamplona.logicode.files.application.BinaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** El adaptador de MinIO contra la misma imagen de docker-compose.yml (P-05, T-85). */
@Testcontainers
class MinioStorageIT {

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("pgsty/minio:RELEASE.2026-08-04T00-00-00Z")
            .withCommand("server", "/data")
            .withEnv("MINIO_ROOT_USER", "logicode")
            .withEnv("MINIO_ROOT_PASSWORD", "clave-de-prueba")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    @TempDir
    Path dir;

    private S3ObjectClient client;
    private LocalBinaryStorage local;
    private S3BinaryStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        client = client("clave-de-prueba");
        client.ensureBucket();
        client.ensureBucket(); // repetirlo no falla
        local = new LocalBinaryStorage(dir);
        storage = new S3BinaryStorage(local, client);
    }

    @Test
    void guardaLeeDesdeUnaPosicionYBorra() throws Exception {
        byte[] content = "0123456789abcdef".repeat(10_000).getBytes(StandardCharsets.UTF_8);
        BinaryStorage.StagedFile staged = storage.stage(new ByteArrayInputStream(content), 1_000_000);
        String key = storage.commit(staged);
        storage.discard(staged);

        assertThat(key).matches("[0-9a-f]{2}/[0-9a-f-]{2}/[0-9a-f-]{36}");
        assertThat(Files.list(dir.resolve(".staging"))).isEmpty();
        Resource resource = storage.open(key);
        assertThat(resource.contentLength()).isEqualTo(content.length);
        try (InputStream in = resource.getInputStream()) {
            assertThat(in.readAllBytes()).isEqualTo(content);
        }
        try (InputStream in = resource.getInputStream()) {
            assertThat(in.skip(150_000)).isEqualTo(150_000);
            assertThat(in.readNBytes(4)).isEqualTo("0123".getBytes(StandardCharsets.UTF_8));
            assertThat(in.readAllBytes()).hasSize(content.length - 150_004);
        }

        storage.delete(key);
        assertThat(client.size(key)).isEmpty();
        assertThatThrownBy(() -> storage.open(key)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaUnaCargaQueNoCoincideConSuSha256() throws Exception {
        Path file = Files.writeString(dir.resolve("x"), "hola");
        assertThatThrownBy(() -> client.put("aa/bb/otro", file, S3ObjectClient.EMPTY_SHA256))
                .hasMessageContaining("HTTP 400");
    }

    @Test
    void rechazaCredencialesEquivocadas() {
        assertThatThrownBy(() -> client("otra-clave").ensureBucket()).hasMessageContaining("HTTP 403");
    }

    @Test
    void pasaAMinioLosArchivosLocalesYLosSigueEntregandoMientrasTanto() throws Exception {
        String key = "0a/1b/0a1b0000-0000-4000-8000-000000000001";
        Path file = dir.resolve(key);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "archivo de antes de MinIO");
        Files.writeString(dir.resolve(".staging/upload-1.part"), "carga en curso");

        try (InputStream in = storage.open(key).getInputStream()) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("archivo de antes de MinIO");
        }
        storage.migrateLocalFiles();
        storage.migrateLocalFiles(); // repetirlo no hace nada

        assertThat(file).doesNotExist();
        assertThat(dir.resolve("0a")).doesNotExist();
        assertThat(dir.resolve(".staging/upload-1.part")).exists();
        try (InputStream in = storage.open(key).getInputStream()) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("archivo de antes de MinIO");
        }
    }

    private static S3ObjectClient client(String secret) {
        URI endpoint = URI.create("http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        return new S3ObjectClient(endpoint, "logicode", "logicode", secret, "us-east-1", Clock.systemUTC());
    }
}
