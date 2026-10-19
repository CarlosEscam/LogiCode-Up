package co.edu.unipamplona.logicode.identity.application;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Textos de los correos transaccionales, en español. */
@Component
public class EmailTemplates {

    private final IdentitySettings settings;

    public EmailTemplates(IdentitySettings settings) {
        this.settings = settings;
    }

    public EmailMessage emailVerification(String toAddress, String fullName, String token) {
        String link = settings.publicUrl() + "/verificar-correo?token=" + token;
        return build(toAddress, fullName, "Confirma tu correo en LogiCode",
                "Para confirmar que este correo te pertenece, abre el siguiente enlace. Vence en 24 horas y solo se puede usar una vez.",
                "Confirmar correo", link,
                "Si no creaste una cuenta en LogiCode ni pediste este cambio, ignora este mensaje.");
    }

    public EmailMessage passwordReset(String toAddress, String fullName, String token) {
        String link = settings.publicUrl() + "/restablecer-contrasena?token=" + token;
        return build(toAddress, fullName, "Restablece tu contraseña de LogiCode",
                "Recibimos una solicitud para restablecer tu contraseña. El enlace vence en 60 minutos y solo se puede usar una vez.",
                "Restablecer contraseña", link,
                "Si no pediste este cambio, ignora este mensaje: tu contraseña actual sigue funcionando.");
    }

    private EmailMessage build(String to, String name, String subject, String intro, String action, String link,
            String footer) {
        String text = "Hola, " + name + ".\n\n" + intro + "\n\n" + link + "\n\n" + footer + "\n\nLogiCode · Universidad de Pamplona";
        String html = "<p>Hola, " + HtmlUtils.htmlEscape(name) + ".</p>"
                + "<p>" + HtmlUtils.htmlEscape(intro) + "</p>"
                + "<p><a href=\"" + HtmlUtils.htmlEscape(link) + "\">" + HtmlUtils.htmlEscape(action) + "</a></p>"
                + "<p style=\"color:#555\">" + HtmlUtils.htmlEscape(footer) + "</p>"
                + "<p>LogiCode · Universidad de Pamplona</p>";
        return new EmailMessage(to, name, subject, text, html);
    }
}
