package co.edu.unipamplona.logicode.quizzes.domain;

/**
 * {@code SINGLE}: una opción correcta. {@code MULTIPLE}: varias; se acierta marcando exactamente las correctas.
 * {@code TRUE_FALSE}: Verdadero o Falso. {@code SHORT}: respuesta corta comparada con las respuestas aceptadas.
 */
public enum QuestionKind {
    SINGLE,
    MULTIPLE,
    TRUE_FALSE,
    SHORT;

    public boolean hasOptions() {
        return this != SHORT;
    }
}
