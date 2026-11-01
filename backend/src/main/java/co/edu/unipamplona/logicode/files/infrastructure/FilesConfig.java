package co.edu.unipamplona.logicode.files.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;

import co.edu.unipamplona.logicode.files.application.BinaryStorage;
import co.edu.unipamplona.logicode.files.application.FilesSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FilesConfig {

    @Bean
    BinaryStorage binaryStorage(FilesProperties properties) throws IOException {
        LocalBinaryStorage local = new LocalBinaryStorage(Path.of(properties.storageDir()));
        if (!"minio".equals(properties.storage())) {
            return local;
        }
        FilesProperties.Minio minio = properties.minio();
        if (minio == null || isBlank(minio.accessKey()) || isBlank(minio.secretKey())) {
            throw new IllegalStateException("Faltan las credenciales de MinIO (MINIO_ROOT_USER y MINIO_ROOT_PASSWORD)");
        }
        S3ObjectClient client = new S3ObjectClient(URI.create(minio.endpoint()), minio.bucket(), minio.accessKey(),
                minio.secretKey(), minio.region(), Clock.systemUTC()); // la firma usa la hora real, no la del negocio
        client.ensureBucket();
        S3BinaryStorage storage = new S3BinaryStorage(local, client);
        // Los archivos anteriores a MinIO se suben en segundo plano; mientras tanto se entregan desde la carpeta.
        Thread.ofVirtual().name("archivos-a-minio").start(storage::migrateLocalFiles);
        return storage;
    }

    @Bean
    FilesSettings filesSettings(FilesProperties properties) {
        return new FilesSettings(properties.retentionMonths());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
