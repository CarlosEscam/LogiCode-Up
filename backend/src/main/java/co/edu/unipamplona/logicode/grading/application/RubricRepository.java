package co.edu.unipamplona.logicode.grading.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.RubricVersion;

public interface RubricRepository {

    RubricVersion save(RubricVersion rubric);

    Optional<RubricVersion> findById(UUID id);

    List<RubricVersion> findAllById(Collection<UUID> ids);

    /** Versión vigente: la de mayor número. */
    Optional<RubricVersion> findCurrent(UUID activityId);

    /** Todas las versiones, de la más reciente a la más antigua. */
    List<RubricVersion> findByActivity(UUID activityId);
}
