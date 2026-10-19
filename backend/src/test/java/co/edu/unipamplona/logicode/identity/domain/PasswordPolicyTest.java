package co.edu.unipamplona.logicode.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"clave#123", "Logi.Code2026", "ñandú_9xx", "12345678$"})
    void acceptsPasswordsMeetingThePolicy(String password) {
        assertThat(PasswordPolicy.violations(password)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cl#1", "clavesinnum#", "clave12345", "        ", ""})
    void rejectsPasswordsBreakingThePolicy(String password) {
        assertThat(PasswordPolicy.isValid(password)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void rejectsPasswordsLongerThanBcryptLimit() {
        assertThat(PasswordPolicy.isValid("a1#" + "x".repeat(80))).isFalse();
    }
}
