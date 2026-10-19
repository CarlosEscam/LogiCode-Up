package co.edu.unipamplona.logicode.files.application;

/**
 * Configuración del módulo de archivos.
 *
 * @param retentionMonths meses mínimos de conservación desde la carga (RN-21: nunca menos de 6)
 */
public record FilesSettings(int retentionMonths) {

    public static final int MIN_RETENTION_MONTHS = 6;

    public FilesSettings {
        if (retentionMonths < MIN_RETENTION_MONTHS) {
            throw new IllegalArgumentException("La conservación mínima de archivos es de " + MIN_RETENTION_MONTHS
                    + " meses (RN-21)");
        }
    }
}
