package co.edu.unipamplona.logicode.pseint.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calificación determinista con casos de prueba (RF-12, RF-16). Un caso pasa si cada línea no vacía de la salida
 * esperada aparece en la salida del programa, en el mismo orden y como palabras completas, sin distinguir mayúsculas
 * ni espacios. Así los mensajes para pedir datos («Ingresa un número:») no hacen fallar un caso.
 */
public final class TestCases {

    public record TestCase(String input, String expectedOutput) {
    }

    public record CaseResult(int number, String input, String expectedOutput, String output, boolean passed,
            String error) {
    }

    /** {@code programError}: el programa no se pudo leer; en ese caso no hay resultados por caso. */
    public record Report(List<CaseResult> cases, String programError, int passed, int total) {

        public boolean runnable() {
            return programError == null;
        }

        /** Nota de 0,0 a 5,0: 5 × casos que pasan ÷ casos, con un decimal. */
        public BigDecimal score() {
            return BigDecimal.valueOf(5L * passed).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        }
    }

    private TestCases() {
    }

    public static Report check(String code, List<TestCase> cases, RunLimits limits) {
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("No hay casos de prueba");
        }
        Pseint.Compiled compiled = Pseint.compile(code, limits);
        if (!compiled.runnable()) {
            return new Report(List.of(), compiled.run("", limits).error(), 0, cases.size());
        }
        List<CaseResult> results = new ArrayList<>();
        int passed = 0;
        for (int i = 0; i < cases.size(); i++) {
            TestCase testCase = cases.get(i);
            RunResult run = compiled.run(testCase.input(), limits);
            boolean ok = run.completed() && matches(testCase.expectedOutput(), run.output());
            if (ok) {
                passed++;
            }
            results.add(new CaseResult(i + 1, testCase.input(), testCase.expectedOutput(), run.output(), ok,
                    run.error()));
        }
        return new Report(results, null, passed, cases.size());
    }

    public static boolean matches(String expected, String actual) {
        String haystack = normalize(actual);
        int from = 0;
        for (String line : expected.split("\n")) {
            String needle = normalize(line);
            if (needle.isEmpty()) {
                continue;
            }
            int found = findWord(haystack, needle, from);
            if (found < 0) {
                return false;
            }
            from = found + needle.length();
        }
        return true;
    }

    private static int findWord(String haystack, String needle, int from) {
        for (int at = haystack.indexOf(needle, from); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            if (startsWord(haystack, at, needle) && endsWord(haystack, at + needle.length(), needle)) {
                return at;
            }
        }
        return -1;
    }

    /** «5» no coincide dentro de «15», «-5» ni «5.5». */
    private static boolean startsWord(String haystack, int at, String needle) {
        if (at == 0) {
            return true;
        }
        char before = haystack.charAt(at - 1);
        return !Character.isLetterOrDigit(before)
                && !(Character.isDigit(needle.charAt(0)) && (before == '-' || before == '.'));
    }

    private static boolean endsWord(String haystack, int end, String needle) {
        if (end == haystack.length()) {
            return true;
        }
        char after = haystack.charAt(end);
        boolean decimalFollows = (after == '.' || after == ',') && end + 1 < haystack.length()
                && Character.isDigit(haystack.charAt(end + 1)) && Character.isDigit(needle.charAt(needle.length() - 1));
        return !Character.isLetterOrDigit(after) && !decimalFollows;
    }

    private static String normalize(String text) {
        return text.replace("\r", "").strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
