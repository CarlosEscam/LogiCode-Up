package co.edu.unipamplona.logicode.identity.infrastructure.mail;

import java.util.List;
import java.util.Map;

import co.edu.unipamplona.logicode.identity.application.EmailMessage;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Entrega correos transaccionales con la API de Brevo. Brevo solo envía: los tokens los genera y valida LogiCode.
 * La API key y el remitente llegan como secretos de Docker.
 */
class BrevoEmailProvider implements EmailProvider {

    private final RestClient client;
    private final String senderEmail;
    private final String senderName;

    BrevoEmailProvider(RestClient.Builder builder, String baseUrl, String apiKey, String senderEmail, String senderName) {
        this.client = builder.baseUrl(baseUrl).defaultHeader("api-key", apiKey).build();
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Override
    public void deliver(EmailMessage message) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", senderEmail, "name", senderName),
                "to", List.of(Map.of("email", message.toAddress(), "name", message.toName())),
                "subject", message.subject(),
                "htmlContent", message.htmlBody(),
                "textContent", message.textBody());
        client.post().uri("/v3/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
