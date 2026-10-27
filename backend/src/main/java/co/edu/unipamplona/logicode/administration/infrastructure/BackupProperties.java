package co.edu.unipamplona.logicode.administration.infrastructure;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param dir carpeta compartida con el servicio {@code backup} (volumen {@code backups} en Docker). */
@ConfigurationProperties(prefix = "logicode.backups")
record BackupProperties(Path dir) {
}
