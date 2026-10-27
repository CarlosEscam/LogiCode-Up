package co.edu.unipamplona.logicode.pseint.domain;

/** Error al interpretar o ejecutar un programa PSeInt. El mensaje se muestra al estudiante. */
public class PseintError extends RuntimeException {

    /**
     * {@code SYNTAX}: el programa no se pudo leer. {@code UNSUPPORTED}: usa algo válido en PSeInt que este intérprete
     * aún no ejecuta. {@code RUNTIME}: error al ejecutar (división por cero, faltan datos...). {@code LIMIT}: superó
     * los pasos, el tiempo, la salida o la memoria permitidos.
     */
    public enum Kind { SYNTAX, UNSUPPORTED, RUNTIME, LIMIT }

    private final Kind kind;
    private final int line;

    public PseintError(Kind kind, int line, String message) {
        super(line > 0 ? "Línea " + line + ": " + message : message);
        this.kind = kind;
        this.line = line;
    }

    public Kind kind() {
        return kind;
    }

    public int line() {
        return line;
    }
}
