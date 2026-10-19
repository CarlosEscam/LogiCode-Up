package co.edu.unipamplona.logicode.identity.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de correo ({@code logicode.mail}).
 *
 * @param provider     {@code outbox} (buzón local de desarrollo) o {@code brevo}
 * @param outboxDir    carpeta del buzón local
 * @param brevoBaseUrl URL base de la API de Brevo
 * @param senderName   nombre visible del remitente
 */
@ConfigurationProperties(prefix = "logicode.mail")
record MailProperties(String provider, String outboxDir, String brevoBaseUrl, String senderName) {

    MailProperties {
        provider = provider == null || provider.isBlank() ? "outbox" : provider.trim().toLowerCase();
        outboxDir = outboxDir == null || outboxDir.isBlank() ? "mail-outbox" : outboxDir;
        brevoBaseUrl = brevoBaseUrl == null || brevoBaseUrl.isBlank() ? "https://api.brevo.com" : brevoBaseUrl;
        senderName = senderName == null || senderName.isBlank() ? "LogiCode" : senderName;
    }
}
