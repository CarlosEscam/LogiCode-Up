package co.edu.unipamplona.logicode.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Textos de los correos transaccionales, en español. */
@Component
public class EmailTemplates {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d/MM/yyyy 'a las' h:mm a", Locale.of("es", "CO"));

    private final IdentitySettings settings;
    private final Clock clock;

    public EmailTemplates(IdentitySettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    /** Fecha y hora en la zona de la plataforma (Colombia), para los textos. */
    public String dateTime(Instant instant) {
        return WHEN.format(instant.atZone(clock.getZone()));
    }

    public EmailMessage emailChange(String toAddress, String fullName, String token, String usableFrom) {
        String link = settings.publicUrl() + "/verificar-correo?token=" + token;
        return build(toAddress, fullName, "Confirma tu correo nuevo en LogiCode",
                "Pediste que este sea el correo de tu cuenta. Por seguridad, el enlace se puede usar desde el "
                        + usableFrom + " (hora de Colombia), vence 48 horas después y solo se puede usar una vez.",
                "Confirmar correo", link,
                "Si no pediste este cambio, ignora este mensaje.");
    }

    /** P-02: aviso al correo actual cuando alguien pide reemplazarlo. */
    public EmailMessage emailChangeNotice(String toAddress, String fullName, String newEmail, String usableFrom) {
        return build(toAddress, fullName, "Pidieron cambiar el correo de tu cuenta de LogiCode",
                "Alguien pidió que tu cuenta use el correo " + mask(newEmail) + " en lugar de este. El cambio se podrá "
                        + "confirmar desde el " + usableFrom + " (hora de Colombia).",
                "Ingresar a LogiCode", settings.publicUrl() + "/ingresar",
                "Si no fuiste tú, ingresa y cambia tu contraseña antes de esa hora: eso cancela el cambio. Si no puedes "
                        + "ingresar, usa \"¿Olvidaste tu contraseña?\": también lo cancela.");
    }

    static String mask(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
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
