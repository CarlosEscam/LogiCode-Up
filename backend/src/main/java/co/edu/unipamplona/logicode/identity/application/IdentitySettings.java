package co.edu.unipamplona.logicode.identity.application;

/**
 * Configuración que necesitan los casos de uso de identidad.
 *
 * @param publicUrl dirección pública de la interfaz web, usada para construir los enlaces de los correos
 */
public record IdentitySettings(String publicUrl) {

    public IdentitySettings {
        if (publicUrl == null || publicUrl.isBlank()) {
            publicUrl = "http://localhost:3000";
        }
        publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }
}
