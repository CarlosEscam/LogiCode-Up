package co.edu.unipamplona.logicode.files.infrastructure;

import java.io.IOException;
import java.nio.file.Path;

import co.edu.unipamplona.logicode.files.application.BinaryStorage;
import co.edu.unipamplona.logicode.files.application.FilesSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FilesConfig {

    @Bean
    BinaryStorage binaryStorage(FilesProperties properties) throws IOException {
        return new LocalBinaryStorage(Path.of(properties.storageDir()));
    }

    @Bean
    FilesSettings filesSettings(FilesProperties properties) {
        return new FilesSettings(properties.retentionMonths());
    }
}
