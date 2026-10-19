package co.edu.unipamplona.logicode.shared.application;

/**
 * Error esperado de un caso de uso. La capa API lo traduce a una respuesta HTTP según su {@link Kind}.
 * El mensaje se muestra al usuario, por lo que debe estar en español y no revelar detalles internos.
 */
public class ApplicationException extends RuntimeException {

    public enum Kind {
        /** 401: se requiere iniciar sesión o las credenciales no son válidas. */
        UNAUTHENTICATED,
        /** 403: la sesión no tiene permiso para la acción. */
        FORBIDDEN,
        /** 404: el recurso no existe. */
        NOT_FOUND,
        /** 409: conflicto con el estado actual (p. ej. cédula ya registrada). */
        CONFLICT,
        /** 422: se incumple una regla de negocio. */
        BUSINESS_RULE,
        /** 429: se superó un límite de solicitudes. */
        TOO_MANY_REQUESTS
    }

    private final Kind kind;

    public ApplicationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static ApplicationException unauthenticated(String message) {
        return new ApplicationException(Kind.UNAUTHENTICATED, message);
    }

    public static ApplicationException forbidden() {
        return new ApplicationException(Kind.FORBIDDEN, "No tienes permiso para realizar esta acción.");
    }

    public static ApplicationException forbidden(String message) {
        return new ApplicationException(Kind.FORBIDDEN, message);
    }

    public static ApplicationException notFound(String message) {
        return new ApplicationException(Kind.NOT_FOUND, message);
    }

    public static ApplicationException conflict(String message) {
        return new ApplicationException(Kind.CONFLICT, message);
    }

    public static ApplicationException businessRule(String message) {
        return new ApplicationException(Kind.BUSINESS_RULE, message);
    }

    public static ApplicationException tooManyRequests(String message) {
        return new ApplicationException(Kind.TOO_MANY_REQUESTS, message);
    }
}
