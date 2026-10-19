package co.edu.unipamplona.logicode.identity.infrastructure.mail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buzón local para desarrollo: guarda cada correo como archivo HTML en una carpeta en lugar de enviarlo.
 * Solo para el entorno local; no se usa cuando Brevo está configurado.
 */
class OutboxEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(OutboxEmailProvider.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path directory;
    private final Clock clock;

    OutboxEmailProvider(Path directory, Clock clock) {
        this.directory = directory;
        this.clock = clock;
    }

    @Override
    public void deliver(EmailMessage message) throws IOException {
        Files.createDirectories(directory);
        String name = STAMP.format(LocalDateTime.now(clock)) + "-" + UUID.randomUUID().toString().substring(0, 8) + ".html";
        String html = "<!doctype html><meta charset=\"utf-8\"><title>" + message.subject() + "</title>"
                + "<p><b>Para:</b> " + message.toAddress() + "<br><b>Asunto:</b> " + message.subject() + "</p><hr>"
                + message.htmlBody();
        Files.writeString(directory.resolve(name), html, StandardCharsets.UTF_8);
        log.info("Correo de desarrollo guardado en el buzón local: {}", name);
    }
}
