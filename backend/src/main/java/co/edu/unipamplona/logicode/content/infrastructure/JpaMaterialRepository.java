package co.edu.unipamplona.logicode.content.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface JpaMaterialRepository extends JpaRepository<Material, UUID>, JpaSpecificationExecutor<Material> {

    List<Material> findByTopicIdInAndArchivedAtIsNullOrderByPositionAscCreatedAtAsc(Collection<UUID> topicIds);

    @Query("select coalesce(max(m.position), 0) from Material m where m.topicId = :topicId")
    int maxPosition(UUID topicId);

    /** Un archivo sigue en uso mientras su material y su tema no estén archivados. */
    @Query("select m.fileId from Material m, Topic t where t.id = m.topicId and m.fileId in :fileIds"
            + " and m.archivedAt is null and t.archivedAt is null")
    Set<UUID> fileIdsInUse(Collection<UUID> fileIds);
}
