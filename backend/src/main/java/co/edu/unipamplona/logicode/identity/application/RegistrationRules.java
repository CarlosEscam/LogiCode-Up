package co.edu.unipamplona.logicode.identity.application;

import java.util.List;

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

    static void requireValidPassword(String password) {
        List<String> problems = PasswordPolicy.violations(password);
        if (!problems.isEmpty()) {
            throw ApplicationException.businessRule("La contraseña no cumple la política: " + String.join(" ", problems));
        }
    }
}
