package co.edu.unipamplona.logicode.identity.infrastructure.mail;

import java.nio.file.Path;
import java.time.Clock;

import co.edu.unipamplona.logicode.identity.application.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.client.RestClient;

/**
 * Elige el proveedor de correo. Con {@code logicode.mail.provider=brevo} exige los secretos
 * {@code brevo_api_key} y {@code brevo_sender_email} (archivos en {@code /run/secrets}).
 */
@Configuration(proxyBeanMethods = false)
class MailConfig {

    @Bean
    EmailSender emailSender(MailProperties properties, Clock clock, TaskExecutor applicationTaskExecutor,
            @Value("${brevo_api_key:}") String brevoApiKey,
            @Value("${brevo_sender_email:}") String brevoSenderEmail) {
        EmailProvider provider = switch (properties.provider()) {
            case "brevo" -> {
                if (brevoApiKey.isBlank() || brevoSenderEmail.isBlank()) {
                    throw new IllegalStateException(
                            "logicode.mail.provider=brevo requiere los secretos brevo_api_key y brevo_sender_email");
                }
                yield new BrevoEmailProvider(RestClient.builder(), properties.brevoBaseUrl(), brevoApiKey.trim(),
                        brevoSenderEmail.trim(), properties.senderName());
            }
            case "outbox" -> new OutboxEmailProvider(Path.of(properties.outboxDir()), clock);
            default -> throw new IllegalStateException("Proveedor de correo desconocido: " + properties.provider());
        };
        return new AsyncAfterCommitEmailSender(provider, applicationTaskExecutor);
    }
}
