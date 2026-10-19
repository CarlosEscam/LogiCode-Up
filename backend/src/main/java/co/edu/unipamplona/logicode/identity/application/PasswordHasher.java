package co.edu.unipamplona.logicode.identity.application;

/** Puerto de hash de contraseñas (BCrypt, decisión D-02). */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
