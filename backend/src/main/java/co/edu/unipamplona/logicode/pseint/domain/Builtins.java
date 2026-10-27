package co.edu.unipamplona.logicode.pseint.domain;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Funciones predefinidas de PSeInt: matemáticas, azar y manejo de textos. */
final class Builtins {

    private Builtins() {
    }

    static Object call(String name, String display, List<Object> args, int line, Random random) {
        return switch (name) {
            case "rc", "raiz" -> {
                double x = number(args, 1, display, line);
                if (x < 0) {
                    throw Interpreter.runtime(line, "no existe la raíz de un número negativo.");
                }
                yield Math.sqrt(x);
            }
            case "abs" -> Math.abs(number(args, 1, display, line));
            case "ln" -> {
                double x = number(args, 1, display, line);
                if (x <= 0) {
                    throw Interpreter.runtime(line, "LN solo recibe números mayores que 0.");
                }
                yield Math.log(x);
            }
            case "exp" -> finite(Math.exp(number(args, 1, display, line)), line);
            case "sen" -> Math.sin(number(args, 1, display, line));
            case "cos" -> Math.cos(number(args, 1, display, line));
            case "tan" -> finite(Math.tan(number(args, 1, display, line)), line);
            case "asen" -> finite(Math.asin(number(args, 1, display, line)), line);
            case "acos" -> finite(Math.acos(number(args, 1, display, line)), line);
            case "atan" -> Math.atan(number(args, 1, display, line));
            case "trunc" -> {
                double x = number(args, 1, display, line);
                yield x < 0 ? Math.ceil(x) : Math.floor(x);
            }
            case "redon" -> {
                double x = number(args, 1, display, line);
                yield Math.signum(x) * Math.floor(Math.abs(x) + 0.5);
            }
            case "azar" -> {
                int n = integer(number(args, 1, display, line), display, line);
                if (n < 1) {
                    throw Interpreter.runtime(line, "AZAR necesita un número mayor que 0.");
                }
                yield (double) random.nextInt(n);
            }
            case "aleatorio" -> {
                int a = integer(number(args, 2, display, line), display, line);
                int b = integer(numberAt(args, 1, display, line), display, line);
                yield (double) Math.min(a, b) + random.nextInt(Math.abs(b - a) + 1);
            }
            case "longitud" -> (double) text(args, 1, display, line).length();
            case "mayusculas" -> text(args, 1, display, line).toUpperCase(Locale.ROOT);
            case "minusculas" -> text(args, 1, display, line).toLowerCase(Locale.ROOT);
            case "subcadena" -> {
                String s = text(args, 3, display, line);
                int from = integer(numberAt(args, 1, display, line), display, line);
                int to = integer(numberAt(args, 2, display, line), display, line);
                int start = Math.max(from, 1) - 1;
                int end = Math.min(to, s.length());
                yield start >= end ? "" : s.substring(start, end);
            }
            case "concatenar" -> {
                count(args, 2, display, line);
                yield Interpreter.text(args.get(0), line) + Interpreter.text(args.get(1), line);
            }
            case "convertiranumero" -> {
                String s = text(args, 1, display, line).strip();
                if (!s.matches("[+-]?\\d+(\\.\\d+)?")) {
                    throw Interpreter.runtime(line, "«" + s + "» no se puede convertir a número.");
                }
                yield Double.parseDouble(s);
            }
            case "convertiratexto" -> Interpreter.format(number(args, 1, display, line));
            default -> throw Interpreter.runtime(line, "no existe la función o subproceso «" + display + "».");
        };
    }

    private static void count(List<Object> args, int expected, String display, int line) {
        if (args.size() != expected) {
            throw Interpreter.runtime(line, "«" + display.toUpperCase(Locale.ROOT) + "» recibe " + expected
                    + " argumento(s).");
        }
    }

    private static double number(List<Object> args, int expected, String display, int line) {
        count(args, expected, display, line);
        return numberAt(args, 0, display, line);
    }

    private static double numberAt(List<Object> args, int index, String display, int line) {
        if (args.get(index) instanceof Double d) {
            return d;
        }
        throw Interpreter.runtime(line, "«" + display.toUpperCase(Locale.ROOT) + "» necesita un número.");
    }

    private static String text(List<Object> args, int expected, String display, int line) {
        count(args, expected, display, line);
        if (args.getFirst() instanceof String s) {
            return s;
        }
        throw Interpreter.runtime(line, "«" + display.toUpperCase(Locale.ROOT) + "» necesita un texto.");
    }

    private static int integer(double value, String display, int line) {
        if (value != Math.floor(value) || Math.abs(value) > Integer.MAX_VALUE / 2.0) {
            throw Interpreter.runtime(line, "«" + display.toUpperCase(Locale.ROOT) + "» necesita un número entero.");
        }
        return (int) value;
    }

    private static double finite(double value, int line) {
        if (!Double.isFinite(value)) {
            throw Interpreter.runtime(line, "el resultado no es un número válido.");
        }
        return value;
    }
}
