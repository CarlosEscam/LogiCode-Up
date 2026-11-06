package co.edu.unipamplona.logicode.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Ley 1581 y guía 13: la línea de log de cada solicitud no lleva la cédula que viaja en la ruta. */
class LogPathsTest {

    static final String COURSE = "3f6c1d2e-8a4b-4c5d-9e0f-112233445566";

    @Test
    void hidesTheCedulaOfAnEnrollment() {
        assertThat(LogPaths.sanitize("/api/v1/courses/" + COURSE + "/enrollments/1000000205"))
                .isEqualTo("/api/v1/courses/" + COURSE + "/enrollments/{cedula}");
    }

    @Test
    void hidesCedulasFromFiveToTwelveDigits() {
        assertThat(LogPaths.sanitize("/api/v1/x/12345")).isEqualTo("/api/v1/x/{cedula}");
        assertThat(LogPaths.sanitize("/api/v1/x/123456789012/y")).isEqualTo("/api/v1/x/{cedula}/y");
    }

    @Test
    void keepsIdentifiersAndShortNumbers() {
        assertThat(LogPaths.sanitize("/api/v1/courses/" + COURSE + "/topics")).isEqualTo("/api/v1/courses/" + COURSE + "/topics");
        assertThat(LogPaths.sanitize("/api/v1/v1/1234")).isEqualTo("/api/v1/v1/1234");
    }
}
