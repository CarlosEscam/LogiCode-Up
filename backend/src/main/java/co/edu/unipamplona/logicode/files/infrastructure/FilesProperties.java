package co.edu.unipamplona.logicode.files.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code logicode.files.*}: dónde se guardan los archivos y meses de conservación (mínimo 6, RN-21).
 *
 * @param storage    {@code local} (carpeta {@code storageDir}) o {@code minio} (P-05; la carpeta queda para las cargas
 *                   en curso y para pasar a MinIO los archivos que había)
 * @param storageDir carpeta local
 * @param minio      conexión a MinIO cuando {@code storage} es {@code minio}
 */
@ConfigurationProperties(prefix = "logicode.files")
record FilesProperties(String storage, String storageDir, int retentionMonths, Minio minio) {

    record Minio(String endpoint, String bucket, String accessKey, String secretKey, String region) {
    }
}
