package co.edu.unipamplona.logicode.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * Equipos conocidos (P-03, pentest 2026-11-11; "device cookies" de OWASP): tras ingresar bien, el navegador recibe
 * una cookie firmada con la cuenta. Con ella, los intentos fallidos de otros no bloquean a esa persona en su equipo,
 * y el límite por IP compartida (Docker Desktop, ngrok) no la deja por fuera.
 * <p>
 * shortcut: la llave se genera al arrancar, así que reiniciar el backend olvida los equipos conocidos; si eso molesta,
 * guardarla en PostgreSQL.
 */
@Component
public class TrustedDevices {

    public static final String COOKIE = "LOGICODE_DEVICE";
    public static final Duration VALIDITY = Duration.ofDays(30);

    private final byte[] key = new byte[32];
    private final Clock clock;

    public TrustedDevices(Clock clock) {
        new SecureRandom().nextBytes(key);
        this.clock = clock;
    }

    /** Valor de la cookie para la cuenta: {@code cuenta.vence.firma}. */
    public String issue(UUID userId) {
        String payload = userId + "." + clock.instant().plus(VALIDITY).getEpochSecond();
        return payload + "." + sign(payload);
    }

    /** La cookie es auténtica, no venció y es de esta cuenta (o de cualquiera, con {@code userId} nulo). */
    public boolean trusts(String value, UUID userId) {
        if (value == null) {
            return false;
        }
        int last = value.lastIndexOf('.');
        int first = value.indexOf('.');
        if (first < 0 || last <= first) {
            return false;
        }
        String payload = value.substring(0, last);
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.US_ASCII),
                value.substring(last + 1).getBytes(StandardCharsets.US_ASCII))) {
            return false;
        }
        try {
            boolean current = Instant.ofEpochSecond(Long.parseLong(value.substring(first + 1, last)))
                    .isAfter(clock.instant());
            return current && (userId == null || userId.toString().equals(value.substring(0, first)));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
