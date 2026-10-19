package co.edu.unipamplona.logicode.content.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.domain.Material;
import co.edu.unipamplona.logicode.content.domain.MaterialKind;
import co.edu.unipamplona.logicode.shared.application.PageResult;

public interface MaterialRepository {

    /**
     * Búsqueda de la biblioteca. Solo materiales no archivados de {@code topicIds}; los privados, solo si su tema
     * está en {@code privateTopicIds}. {@code text} (opcional) se busca en título y descripción del material, o el
     * material pertenece a uno de {@code textTopicIds}.
     */
    record Search(Set<UUID> topicIds, Set<UUID> privateTopicIds, MaterialKind kind, String text,
            Set<UUID> textTopicIds) {
    }

    Material save(Material material);

    Optional<Material> findById(UUID id);

    /** Materiales no archivados de los temas dados, en su orden. */
    List<Material> findActiveByTopics(Collection<UUID> topicIds);

    int nextPosition(UUID topicId);

    PageResult<Material> search(Search search, int page, int size);

    /** Temas (de {@code search.topicIds()}) que tienen por lo menos un material visible según {@code search}. */
    Set<UUID> topicsWithMaterials(Search search);
}
