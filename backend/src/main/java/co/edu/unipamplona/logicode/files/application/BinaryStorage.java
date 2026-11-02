package co.edu.unipamplona.logicode.files.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.springframework.core.io.Resource;

/** Puerto hacia donde se guarda el contenido de los archivos: MinIO en Docker (P-05) o una carpeta local. */
public interface BinaryStorage {

    /**
     * Copia el flujo a un área temporal calculando tamaño y SHA-256. Deja de leer y falla con
     * {@link TooLargeException} si supera {@code maxBytes}.
     */
    StagedFile stage(InputStream content, long maxBytes) throws IOException;

    /** Nombres de las entradas de un archivo ZIP en preparación (para reconocer DOCX, PPTX y SB3). */
    Set<String> zipEntryNames(StagedFile staged);

    /** Mueve el archivo preparado a su ubicación definitiva y devuelve la clave aleatoria. */
    String commit(StagedFile staged) throws IOException;

    void discard(StagedFile staged);

    /** Borra un archivo ya guardado: deshace una carga fallida o borra uno vencido. No falla si ya no existe. */
    void delete(String storageKey);

    Resource open(String storageKey);

    record StagedFile(Object handle, long size, String sha256, byte[] head) {
    }

    final class TooLargeException extends IOException {
        public TooLargeException() {
            super("El archivo supera el tamaño máximo");
        }
    }
}
