package co.edu.unipamplona.logicode.pseint.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Intérprete de PSeInt de LogiCode (RF-16, RNF-05). Lee el programa una vez y lo ejecuta con las entradas dadas
 * (una por línea, en el orden de los «Leer»), dentro de los límites indicados.
 */
public final class Pseint {

    /** Programa listo para ejecutarse varias veces, o el error que impidió leerlo. */
    public static final class Compiled {

        private final Ast.Program program;
        private final PseintError error;

        private Compiled(Ast.Program program, PseintError error) {
            this.program = program;
            this.error = error;
        }

        public boolean runnable() {
            return error == null;
        }

        public RunResult run(String input, RunLimits limits) {
            if (error != null) {
                return new RunResult("", error.kind(), error.getMessage(), 0);
            }
            if (input != null && input.length() > limits.maxInputChars()) {
                return new RunResult("", PseintError.Kind.LIMIT, "Las entradas superan el máximo de "
                        + limits.maxInputChars() + " caracteres.", 0);
            }
            return Interpreter.run(program, lines(input), limits);
        }
    }

    private Pseint() {
    }

    public static Compiled compile(String code, RunLimits limits) {
        if (code == null || code.isBlank()) {
            return new Compiled(null, new PseintError(PseintError.Kind.SYNTAX, 0, "El programa está vacío."));
        }
        if (code.length() > limits.maxCodeChars()) {
            return new Compiled(null, new PseintError(PseintError.Kind.LIMIT, 0, "El programa supera el máximo de "
                    + limits.maxCodeChars() + " caracteres."));
        }
        try {
            return new Compiled(Parser.parse(code.replace("\r", "")), null);
        } catch (PseintError e) {
            return new Compiled(null, e);
        }
    }

    public static RunResult run(String code, String input, RunLimits limits) {
        return compile(code, limits).run(input, limits);
    }

    /** Una entrada por línea; el salto final no cuenta como una entrada vacía. */
    private static List<String> lines(String input) {
        List<String> lines = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return lines;
        }
        String text = input.replace("\r", "");
        if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        for (String line : text.split("\n", -1)) {
            lines.add(line);
        }
        return lines;
    }
}
