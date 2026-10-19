package co.edu.unipamplona.logicode.files.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code logicode.files.*}: carpeta del almacenamiento local y meses de conservación (mínimo 6, RN-21).
 */
@ConfigurationProperties(prefix = "logicode.files")
record FilesProperties(String storageDir, int retentionMonths) {
}
