package co.edu.unipamplona.logicode.grading.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Caso de prueba de PSeInt (RF-12): entradas, una por línea, y lo que la salida debe mostrar. */
@Embeddable
public class RubricTestCase {

    @Column(name = "input_data", nullable = false)
    private String input;

    @Column(name = "expected_output", nullable = false)
    private String expectedOutput;

    protected RubricTestCase() {
    }

    public RubricTestCase(String input, String expectedOutput) {
        if (expectedOutput == null || expectedOutput.isBlank()) {
            throw new IllegalArgumentException("El caso de prueba necesita la salida esperada");
        }
        this.input = input == null ? "" : input;
        this.expectedOutput = expectedOutput;
    }

    public String getInput() {
        return input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }
}
