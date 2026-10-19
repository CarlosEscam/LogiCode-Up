package co.edu.unipamplona.logicode.content.domain;

/** Tipo de material (RF-07). Todos son archivos cargados, salvo {@link #LINK}. */
public enum MaterialKind {
    GUIDE,
    DOCUMENT,
    PRESENTATION,
    IMAGE,
    VIDEO,
    /** Archivo de PSeInt, Arduino o Scratch. */
    TOOL_FILE,
    LINK;

    public boolean isLink() {
        return this == LINK;
    }
}
