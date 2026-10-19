package co.edu.unipamplona.logicode.content.application;

import co.edu.unipamplona.logicode.content.domain.MaterialKind;
import co.edu.unipamplona.logicode.files.application.AcceptedFiles;

/** Formatos de archivo aceptados según el tipo de material (RNF-04). */
final class MaterialFormats {

    private MaterialFormats() {
    }

    static AcceptedFiles acceptedFor(MaterialKind kind) {
        return switch (kind) {
            case GUIDE, DOCUMENT -> AcceptedFiles.DOCUMENTS;
            case PRESENTATION -> AcceptedFiles.PRESENTATIONS;
            case IMAGE -> AcceptedFiles.IMAGES;
            case VIDEO -> AcceptedFiles.VIDEOS;
            case TOOL_FILE -> AcceptedFiles.TOOL_FILES;
            case LINK -> throw new IllegalArgumentException("Un enlace no lleva archivo");
        };
    }
}
