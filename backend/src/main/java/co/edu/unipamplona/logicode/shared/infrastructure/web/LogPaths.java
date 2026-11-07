package co.edu.unipamplona.logicode.shared.infrastructure.web;

import java.util.regex.Pattern;

/**
 * Ruta de una solicitud tal como se escribe en el log. Las cédulas viajan en algunas rutas
 * ({@code DELETE /courses/{id}/enrollments/{cedula}}); los identificadores son UUID, así que un tramo de solo
 * dígitos (5 a 12) es una cédula y no se registra (Ley 1581, T-97).
 */
final class LogPaths {

    private static final Pattern CEDULA_SEGMENT = Pattern.compile("/[0-9]{5,12}(?=/|$)");

    private LogPaths() {
    }

    static String sanitize(String path) {
        return CEDULA_SEGMENT.matcher(path).replaceAll("/{cedula}");
    }
}
