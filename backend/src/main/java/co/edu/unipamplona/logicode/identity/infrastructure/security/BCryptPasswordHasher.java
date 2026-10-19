package co.edu.unipamplona.logicode.identity.infrastructure.security;

import co.edu.unipamplona.logicode.identity.application.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Hash de contraseñas con BCrypt, costo 12 (decisión D-02). */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    static final int STRENGTH = 12;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return hash != null && encoder.matches(rawPassword, hash);
    }
}
