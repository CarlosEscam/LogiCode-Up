package co.edu.unipamplona.logicode.identity.domain;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Política de contraseñas (RNF-02): mínimo 8 caracteres, al menos un número y un carácter especial.
 * El máximo de 72 bytes lo impone BCrypt, que ignora lo que sigue.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /** Devuelve los incumplimientos en español; una lista vacía significa que la contraseña es válida. */
    public static List<String> violations(String password) {
        List<String> problems = new ArrayList<>();
        if (password == null) {
            problems.add("La contraseña es obligatoria.");
            return problems;
        }
        if (password.codePointCount(0, password.length()) < MIN_LENGTH) {
            problems.add("Debe tener al menos " + MIN_LENGTH + " caracteres.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            problems.add("Es demasiado larga (máximo " + MAX_BYTES + " bytes).");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            problems.add("Debe incluir al menos un número.");
        }
        if (password.chars().noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
            problems.add("Debe incluir al menos un carácter especial.");
        }
        return problems;
    }

    public static boolean isValid(String password) {
        return violations(password).isEmpty();
    }
}
