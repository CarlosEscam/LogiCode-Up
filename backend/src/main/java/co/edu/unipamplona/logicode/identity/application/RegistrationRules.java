package co.edu.unipamplona.logicode.identity.application;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.identity.domain.Cedula;
import co.edu.unipamplona.logicode.identity.domain.EmailAddress;
import co.edu.unipamplona.logicode.identity.domain.PasswordPolicy;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;

/** Validaciones comunes de datos de registro. */
final class RegistrationRules {

    private RegistrationRules() {
    }

    static String requireCedula(String raw) {
        String cedula = Cedula.normalize(raw);
        if (cedula == null) {
            throw ApplicationException.businessRule("La cédula debe contener solo números.");
        }
        return cedula;
    }

    static String requireEmail(String raw) {
        String email = EmailAddress.normalize(raw);
        if (email == null) {
            throw ApplicationException.businessRule("El correo no tiene un formato válido.");
        }
        return email;
    }

    static String requireFullName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 150) {
            throw ApplicationException.businessRule("Escribe tu nombre completo.");
        }
        return name;
    }

    /**
     * P-01 (pentest 2026-11-11): el nombre del registro debe contener todas las palabras del nombre que el docente
     * habilitó, sin importar tildes ni mayúsculas; así una cédula ajena no basta para quedarse con el cupo. Sin nombre
     * en la habilitación (lista solo con cédulas) no hay con qué comparar.
     */
    static void requireEnabledName(String enabledName, String givenName) {
        Set<String> given = words(givenName);
        if (enabledName != null && !given.containsAll(words(enabledName))) {
            throw ApplicationException.businessRule(
                    "El nombre no coincide con el de la lista de tu docente. Escríbelo completo, como aparece en la lista del curso.");
        }
    }

    private static Set<String> words(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(plain.split("[^\\p{L}\\p{N}]+")).filter(w -> !w.isEmpty()).collect(Collectors.toSet());
    }

    static void requireValidPassword(String password) {
        List<String> problems = PasswordPolicy.violations(password);
        if (!problems.isEmpty()) {
            throw ApplicationException.businessRule("La contraseña no cumple la política: " + String.join(" ", problems));
        }
    }
}
