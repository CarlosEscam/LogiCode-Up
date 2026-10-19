package co.edu.unipamplona.logicode.files.application;

import java.util.EnumSet;
import java.util.Set;

import co.edu.unipamplona.logicode.files.domain.FileFormat;

/** Grupos de formatos que otros módulos pueden aceptar al cargar un archivo (RNF-04). */
public enum AcceptedFiles {
    DOCUMENTS(EnumSet.of(FileFormat.PDF, FileFormat.DOC, FileFormat.DOCX)),
    PRESENTATIONS(EnumSet.of(FileFormat.PDF, FileFormat.PPT, FileFormat.PPTX)),
    IMAGES(EnumSet.of(FileFormat.PNG, FileFormat.JPG)),
    VIDEOS(EnumSet.of(FileFormat.MP4, FileFormat.MOV)),
    /** Archivos de PSeInt, Arduino y Scratch. */
    TOOL_FILES(EnumSet.of(FileFormat.PSC, FileFormat.INO, FileFormat.SB3));

    private final Set<FileFormat> formats;

    AcceptedFiles(Set<FileFormat> formats) {
        this.formats = formats;
    }

    Set<FileFormat> formats() {
        return formats;
    }
}
