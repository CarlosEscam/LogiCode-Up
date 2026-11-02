package co.edu.unipamplona.logicode.files.application;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Lo implementa cada módulo que guarda ids de archivos. Un archivo vencido se borra solo si ningún módulo lo
 * reporta en uso.
 */
public interface FileUsage {

    /** De {@code fileIds}, los que este módulo todavía muestra o necesita. */
    Set<UUID> inUse(Collection<UUID> fileIds);
}
