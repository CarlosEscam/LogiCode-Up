package co.edu.unipamplona.logicode.identity.application;

/** Datos técnicos de la solicitud en curso para la auditoría. */
public interface RequestContext {

    /** Identificador de la solicitud ({@code X-Request-Id}) o {@code null} fuera de una solicitud HTTP. */
    String requestId();
}
