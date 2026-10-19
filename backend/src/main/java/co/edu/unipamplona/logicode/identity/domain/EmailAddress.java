package co.edu.unipamplona.logicode.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Validación y normalización simple de direcciones de correo. */
public final class EmailAddress {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]{1,64}@[^\\s@]+\\.[^\\s@]{2,}$");

    private EmailAddress() {
    }

    /** Devuelve el correo en minúsculas y sin espacios, o {@code null} si el formato no es válido. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().toLowerCase(Locale.ROOT);
        return cleaned.length() <= 254 && FORMAT.matcher(cleaned).matches() ? cleaned : null;
    }
}
