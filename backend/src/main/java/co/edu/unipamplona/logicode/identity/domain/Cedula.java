package co.edu.unipamplona.logicode.identity.domain;

import java.util.regex.Pattern;

/** Cédula numérica que identifica una cuenta (RN-01). */
public final class Cedula {

    private static final Pattern DIGITS = Pattern.compile("[0-9]{3,15}");

    private Cedula() {
    }

    /** Quita espacios, puntos y guiones habituales al escribir la cédula; devuelve {@code null} si no es válida. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[\\s.\\-]", "");
        return DIGITS.matcher(cleaned).matches() ? cleaned : null;
    }
}
