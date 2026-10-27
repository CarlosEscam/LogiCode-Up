package co.edu.unipamplona.logicode.pseint.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Ejecuta el árbol del programa sin tocar nada fuera de él: lee las entradas dadas, escribe en un texto y se detiene
 * al pasar cualquiera de los {@link RunLimits}. Los números son reales de doble precisión como en PSeInt; los
 * arreglos aceptan índices desde 0 hasta su tamaño para no castigar la base (0 o 1) que use cada curso.
 */
final class Interpreter {

    private static final Pattern NUMBER = Pattern.compile("[+-]?\\d+(\\.\\d+)?");
    private static final MathContext PRECISION = new MathContext(12);
    /** AZAR y ALEATORIO con semilla fija: el mismo programa da la misma salida en cada caso de prueba. */
    private static final long SEED = 20_261L;

    private static final class Slot {
        Object value;
        String type;
    }

    private record ArrayValue(int[] sizes, Object[] cells) {
    }

    private final Ast.Program program;
    private final RunLimits limits;
    private final Deque<String> input;
    private final StringBuilder output = new StringBuilder();
    private final Random random = new Random(SEED);
    private final long deadline;
    private long steps;
    private int cells;
    private int callDepth;

    private Interpreter(Ast.Program program, RunLimits limits, List<String> input) {
        this.program = program;
        this.limits = limits;
        this.input = new ArrayDeque<>(input);
        this.deadline = System.nanoTime() + limits.maxMillis() * 1_000_000;
    }

    static RunResult run(Ast.Program program, List<String> input, RunLimits limits) {
        Interpreter interpreter = new Interpreter(program, limits, input);
        try {
            interpreter.execute(program.main(), new HashMap<>());
            return new RunResult(interpreter.output.toString(), null, null, interpreter.steps);
        } catch (PseintError e) {
            return new RunResult(interpreter.output.toString(), e.kind(), e.getMessage(), interpreter.steps);
        } catch (StackOverflowError e) {
            return new RunResult(interpreter.output.toString(), PseintError.Kind.LIMIT,
                    "El programa hizo demasiadas llamadas anidadas.", interpreter.steps);
        }
    }

    private void execute(List<Ast.Statement> statements, Map<String, Slot> frame) {
        for (Ast.Statement statement : statements) {
            step(statement.line());
            switch (statement) {
                case Ast.Define d -> d.names().forEach(name -> frame.computeIfAbsent(name, n -> new Slot()).type = d.type());
                case Ast.Dimension d -> dimension(d, frame);
                case Ast.Assign a -> store(a.target(), evaluate(a.value(), frame), frame);
                case Ast.Write w -> write(w, frame);
                case Ast.Read r -> read(r, frame);
                case Ast.If i -> execute(condition(i.condition(), frame) ? i.then() : i.otherwise(), frame);
                case Ast.Switch s -> select(s, frame);
                case Ast.While w -> {
                    while (condition(w.condition(), frame)) {
                        execute(w.body(), frame);
                        step(w.line());
                    }
                }
                case Ast.Repeat r -> {
                    do {
                        execute(r.body(), frame);
                        step(r.line());
                    } while (condition(r.condition(), frame) != r.untilTrue());
                }
                case Ast.For f -> loop(f, frame);
                case Ast.CallStatement c -> call(c.call(), frame, false);
                case Ast.Nothing n -> {
                    // Sin pantalla que borrar ni tecla que esperar.
                }
            }
        }
    }

    private void step(int line) {
        steps++;
        if (steps > limits.maxSteps()) {
            throw new PseintError(PseintError.Kind.LIMIT, line, "el programa superó el máximo de "
                    + limits.maxSteps() + " pasos. ¿Hay un ciclo que nunca termina?");
        }
        if ((steps & 255) == 0 && (System.nanoTime() > deadline || Thread.currentThread().isInterrupted())) {
            throw new PseintError(PseintError.Kind.LIMIT, line, "el programa superó el tiempo máximo de "
                    + limits.maxMillis() / 1000.0 + " s.");
        }
    }

    private void dimension(Ast.Dimension d, Map<String, Slot> frame) {
        int[] sizes = new int[d.sizes().size()];
        long total = 1;
        for (int i = 0; i < sizes.length; i++) {
            double size = number(evaluate(d.sizes().get(i), frame), d.line());
            if (size < 1 || size != Math.floor(size)) {
                throw runtime(d.line(), "el tamaño del arreglo «" + d.name() + "» debe ser un entero mayor que 0.");
            }
            sizes[i] = (int) Math.min(size, Integer.MAX_VALUE - 1);
            total *= sizes[i] + 1L;
            if (cells + total > limits.maxArrayCells()) {
                throw new PseintError(PseintError.Kind.LIMIT, d.line(), "los arreglos superan el máximo de "
                        + limits.maxArrayCells() + " posiciones.");
            }
        }
        cells += (int) total;
        frame.computeIfAbsent(d.name(), n -> new Slot()).value = new ArrayValue(sizes, new Object[(int) total]);
    }

    private void write(Ast.Write w, Map<String, Slot> frame) {
        for (Ast.Expression value : w.values()) {
            output.append(text(evaluate(value, frame), value.line()));
        }
        if (w.newLine()) {
            output.append('\n');
        }
        if (output.length() > limits.maxOutputChars()) {
            output.setLength(limits.maxOutputChars());
            throw new PseintError(PseintError.Kind.LIMIT, w.line(), "la salida superó el máximo de "
                    + limits.maxOutputChars() + " caracteres.");
        }
    }

    private void read(Ast.Read r, Map<String, Slot> frame) {
        for (Ast.Variable target : r.targets()) {
            if (input.isEmpty()) {
                throw runtime(r.line(), "faltan datos de entrada para «Leer " + target.display() + "».");
            }
            String line = input.poll().strip();
            Slot slot = frame.get(target.name());
            String type = slot == null ? null : slot.type;
            store(target, convertInput(line, type, r.line()), frame);
        }
    }

    private static Object convertInput(String line, String type, int at) {
        boolean isNumber = NUMBER.matcher(line).matches();
        if (type == null) {
            return isNumber ? Double.parseDouble(line) : line;
        }
        return switch (type) {
            case "entero", "real", "numero", "numerico" -> {
                if (!isNumber) {
                    throw runtime(at, "se esperaba un número y se leyó «" + line + "».");
                }
                yield Double.parseDouble(line);
            }
            case "logico" -> switch (Lexer.normalize(line)) {
                case "verdadero", "v" -> true;
                case "falso", "f" -> false;
                default -> throw runtime(at, "se esperaba Verdadero o Falso y se leyó «" + line + "».");
            };
            default -> line;
        };
    }

    private void select(Ast.Switch s, Map<String, Slot> frame) {
        Object value = evaluate(s.value(), frame);
        for (Ast.Case option : s.cases()) {
            for (Ast.Expression candidate : option.values()) {
                if (compare("=", value, evaluate(candidate, frame), candidate.line())) {
                    execute(option.body(), frame);
                    return;
                }
            }
        }
        execute(s.otherwise(), frame);
    }

    private void loop(Ast.For f, Map<String, Slot> frame) {
        double to = number(evaluate(f.to(), frame), f.line());
        double increment = f.step() == null ? 1 : number(evaluate(f.step(), frame), f.line());
        if (increment == 0) {
            throw runtime(f.line(), "el paso del ciclo Para no puede ser 0.");
        }
        Slot slot = frame.computeIfAbsent(f.variable(), n -> new Slot());
        slot.value = number(evaluate(f.from(), frame), f.line());
        while (increment > 0 ? number(slot.value, f.line()) <= to : number(slot.value, f.line()) >= to) {
            execute(f.body(), frame);
            step(f.line());
            slot.value = number(slot.value, f.line()) + increment;
        }
    }

    private Object evaluate(Ast.Expression expression, Map<String, Slot> frame) {
        return switch (expression) {
            case Ast.Literal l -> l.value();
            case Ast.Variable v -> load(v, frame);
            case Ast.Unary u -> switch (u.operator()) {
                case "no" -> !condition(u.operand(), frame);
                case "-" -> -number(evaluate(u.operand(), frame), u.line());
                default -> number(evaluate(u.operand(), frame), u.line());
            };
            case Ast.Binary b -> binary(b, frame);
            case Ast.Call c -> call(c, frame, true);
        };
    }

    private Object binary(Ast.Binary b, Map<String, Slot> frame) {
        int line = b.line();
        if (b.operator().equals("y")) {
            return condition(b.left(), frame) && condition(b.right(), frame);
        }
        if (b.operator().equals("o")) {
            return condition(b.left(), frame) || condition(b.right(), frame);
        }
        Object left = evaluate(b.left(), frame);
        Object right = evaluate(b.right(), frame);
        if (b.operator().equals("+") && (left instanceof String || right instanceof String)) {
            return limitText(text(left, line) + text(right, line), line);
        }
        return switch (b.operator()) {
            case "=", "<>", "<", ">", "<=", ">=" -> compare(b.operator(), left, right, line);
            default -> arithmetic(b.operator(), number(left, line), number(right, line), line);
        };
    }

    private static double arithmetic(String operator, double left, double right, int line) {
        double result = switch (operator) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> {
                if (right == 0) {
                    throw runtime(line, "división por cero.");
                }
                yield left / right;
            }
            case "%" -> {
                if (right == 0) {
                    throw runtime(line, "MOD por cero.");
                }
                yield left % right;
            }
            default -> Math.pow(left, right);
        };
        if (!Double.isFinite(result)) {
            throw runtime(line, "el resultado no es un número válido.");
        }
        return result;
    }

    private static boolean compare(String operator, Object left, Object right, int line) {
        int order;
        if (left instanceof Double a && right instanceof Double b) {
            order = Double.compare(a, b);
        } else if (left instanceof String a && right instanceof String b) {
            order = a.compareTo(b);
        } else if (left instanceof Boolean a && right instanceof Boolean b
                && (operator.equals("=") || operator.equals("<>"))) {
            order = a.equals(b) ? 0 : 1;
        } else {
            throw runtime(line, "no se puede comparar " + kind(left) + " con " + kind(right) + ".");
        }
        return switch (operator) {
            case "=" -> order == 0;
            case "<>" -> order != 0;
            case "<" -> order < 0;
            case ">" -> order > 0;
            case "<=" -> order <= 0;
            default -> order >= 0;
        };
    }

    private Object call(Ast.Call c, Map<String, Slot> frame, boolean needsValue) {
        Ast.Function function = program.functions().get(c.name());
        if (function == null) {
            return Builtins.call(c.name(), c.display(), c.arguments().stream()
                    .map(argument -> evaluate(argument, frame)).toList(), c.line(), random);
        }
        if (c.arguments().size() != function.params().size()) {
            throw runtime(c.line(), "«" + function.name() + "» recibe " + function.params().size()
                    + " argumento(s) y se le dieron " + c.arguments().size() + ".");
        }
        if (++callDepth > limits.maxCallDepth()) {
            throw new PseintError(PseintError.Kind.LIMIT, c.line(), "demasiadas llamadas anidadas a «"
                    + function.name() + "». ¿La recursión nunca termina?");
        }
        try {
            Map<String, Slot> local = new HashMap<>();
            for (int i = 0; i < function.params().size(); i++) {
                Ast.Param param = function.params().get(i);
                Ast.Expression argument = c.arguments().get(i);
                if (param.byReference()) {
                    if (!(argument instanceof Ast.Variable v) || !v.indices().isEmpty()) {
                        throw runtime(c.line(), "el parámetro «" + param.name()
                                + "» es Por Referencia: pasa una variable.");
                    }
                    local.put(param.name(), frame.computeIfAbsent(v.name(), n -> new Slot()));
                } else {
                    Slot slot = new Slot();
                    slot.value = evaluate(argument, frame);
                    local.put(param.name(), slot);
                }
            }
            if (function.returnVar() != null) {
                local.computeIfAbsent(function.returnVar(), n -> new Slot());
            }
            execute(function.body(), local);
            if (!needsValue) {
                return null;
            }
            if (function.returnVar() == null) {
                throw runtime(c.line(), "«" + function.name() + "» no devuelve un valor.");
            }
            Object result = local.get(function.returnVar()).value;
            if (result == null) {
                throw runtime(c.line(), "«" + function.name() + "» no asignó su valor de retorno.");
            }
            return result;
        } finally {
            callDepth--;
        }
    }

    private Object load(Ast.Variable v, Map<String, Slot> frame) {
        Slot slot = frame.get(v.name());
        if (slot == null || slot.value == null) {
            if (v.name().equals("pi") && v.indices().isEmpty()) {
                return Math.PI;
            }
            throw runtime(v.line(), "la variable «" + v.display() + "» no tiene valor.");
        }
        if (v.indices().isEmpty()) {
            return slot.value;
        }
        ArrayValue array = array(slot, v);
        Object value = array.cells()[offset(array, v, frame)];
        if (value == null) {
            throw runtime(v.line(), "la posición indicada de «" + v.display() + "» no tiene valor.");
        }
        return value;
    }

    private void store(Ast.Variable target, Object value, Map<String, Slot> frame) {
        if (value instanceof String s) {
            limitText(s, target.line());
        }
        Slot slot = frame.computeIfAbsent(target.name(), n -> new Slot());
        if (target.indices().isEmpty()) {
            if (slot.value instanceof ArrayValue && !(value instanceof ArrayValue)) {
                throw runtime(target.line(), "«" + target.display() + "» es un arreglo: indica la posición.");
            }
            slot.value = value;
            return;
        }
        if (value instanceof ArrayValue) {
            throw runtime(target.line(), "no se puede guardar un arreglo dentro de otro.");
        }
        ArrayValue array = array(slot, target);
        array.cells()[offset(array, target, frame)] = value;
    }

    private static ArrayValue array(Slot slot, Ast.Variable v) {
        if (!(slot.value instanceof ArrayValue array)) {
            throw runtime(v.line(), "«" + v.display() + "» no es un arreglo (falta «Dimension " + v.display()
                    + "[…]»).");
        }
        return array;
    }

    private int offset(ArrayValue array, Ast.Variable v, Map<String, Slot> frame) {
        if (v.indices().size() != array.sizes().length) {
            throw runtime(v.line(), "«" + v.display() + "» tiene " + array.sizes().length + " dimensión(es).");
        }
        int offset = 0;
        for (int i = 0; i < array.sizes().length; i++) {
            double index = number(evaluate(v.indices().get(i), frame), v.line());
            if (index != Math.floor(index) || index < 0 || index > array.sizes()[i]) {
                throw runtime(v.line(), "el índice " + format(index) + " está fuera de «" + v.display()
                        + "» (tamaño " + array.sizes()[i] + ").");
            }
            offset = offset * (array.sizes()[i] + 1) + (int) index;
        }
        return offset;
    }

    private boolean condition(Ast.Expression expression, Map<String, Slot> frame) {
        Object value = evaluate(expression, frame);
        if (value instanceof Boolean b) {
            return b;
        }
        throw runtime(expression.line(), "se esperaba una condición (Verdadero o Falso) y se obtuvo "
                + kind(value) + ".");
    }

    private static double number(Object value, int line) {
        if (value instanceof Double d) {
            return d;
        }
        throw runtime(line, "se esperaba un número y se obtuvo " + kind(value) + ".");
    }

    static String text(Object value, int line) {
        return switch (value) {
            case Double d -> format(d);
            case Boolean b -> b ? "VERDADERO" : "FALSO";
            case String s -> s;
            default -> throw runtime(line, "no se puede escribir un arreglo completo; recórrelo con un ciclo.");
        };
    }

    private String limitText(String value, int line) {
        if (value.length() > limits.maxTextChars()) {
            throw new PseintError(PseintError.Kind.LIMIT, line, "un texto superó el máximo de "
                    + limits.maxTextChars() + " caracteres.");
        }
        return value;
    }

    /** Enteros sin decimales y reales con hasta 12 cifras significativas, como los muestra PSeInt. */
    static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return new BigDecimal(value).round(PRECISION).stripTrailingZeros().toPlainString();
    }

    private static String kind(Object value) {
        return switch (value) {
            case Double d -> "un número";
            case String s -> "un texto";
            case Boolean b -> "un valor lógico";
            case null -> "nada";
            default -> "un arreglo";
        };
    }

    static PseintError runtime(int line, String message) {
        return new PseintError(PseintError.Kind.RUNTIME, line, message);
    }
}
