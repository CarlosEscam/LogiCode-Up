package co.edu.unipamplona.logicode.identity.application;

/** Datos de una cuenta inicial con privilegio completo, leídos de la configuración local (D-03). */
public record InitialAdmin(String cedula, String fullName, String email) {

    public boolean isConfigured() {
        return cedula != null && !cedula.isBlank();
    }
}
