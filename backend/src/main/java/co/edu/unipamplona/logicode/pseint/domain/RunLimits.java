package co.edu.unipamplona.logicode.pseint.domain;

/**
 * Límites de una ejecución (RNF-05). El lenguaje no tiene instrucciones para archivos, red ni el sistema, así que el
 * programa solo puede consumir pasos, tiempo, salida y memoria; estos límites acotan los cuatro.
 */
public record RunLimits(int maxCodeChars, int maxInputChars, long maxSteps, long maxMillis, int maxOutputChars,
        int maxArrayCells, int maxTextChars, int maxCallDepth) {

    /** Alcanza para los ejercicios de primer semestre y corta un ciclo infinito en milisegundos. */
    public static final RunLimits DEFAULT = new RunLimits(20_000, 10_000, 1_000_000, 2_000, 20_000, 10_000, 1_000,
            200);
}
