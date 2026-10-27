package co.edu.unipamplona.logicode.pseint.domain;

/** Resultado de ejecutar un programa: la salida hasta donde llegó y, si se detuvo, el error. */
public record RunResult(String output, PseintError.Kind errorKind, String error, long steps) {

    public boolean completed() {
        return errorKind == null;
    }

    /** Errores de lectura del programa: no se pudo ejecutar ni una instrucción. */
    public boolean runnable() {
        return errorKind != PseintError.Kind.SYNTAX && errorKind != PseintError.Kind.UNSUPPORTED;
    }
}
