package co.edu.unipamplona.logicode.content.infrastructure;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.files.application.FileUsage;
import org.springframework.stereotype.Component;

/** Los archivos de materiales archivados (o de temas archivados) ya no se muestran a nadie y pueden borrarse. */
@Component
class MaterialFileUsage implements FileUsage {

    private final JpaMaterialRepository materials;

    MaterialFileUsage(JpaMaterialRepository materials) {
        this.materials = materials;
    }

    @Override
    public Set<UUID> inUse(Collection<UUID> fileIds) {
        return materials.fileIdsInUse(fileIds);
    }
}
