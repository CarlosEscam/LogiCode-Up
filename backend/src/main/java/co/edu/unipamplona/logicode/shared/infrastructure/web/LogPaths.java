package co.edu.unipamplona.logicode.shared.infrastructure.web;

/** Ruta de una solicitud tal como se escribe en el log. */
final class LogPaths {

    private LogPaths() {
    }

    static String sanitize(String path) {
        return path.replaceAll("/[0-9]{5,12}(?=/|$)", "/{cedula}");
    }
}
