package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.application.RubricRepository;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import org.springframework.stereotype.Repository;

@Repository
class RubricRepositoryAdapter implements RubricRepository {

    private final JpaRubricRepository jpa;

    RubricRepositoryAdapter(JpaRubricRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RubricVersion save(RubricVersion rubric) {
        return jpa.save(rubric);
    }

    @Override
    public Optional<RubricVersion> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<RubricVersion> findAllById(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllById(ids);
    }

    @Override
    public Optional<RubricVersion> findCurrent(UUID activityId) {
        return jpa.findFirstByActivityIdOrderByNumberDesc(activityId);
    }

    @Override
    public List<RubricVersion> findByActivity(UUID activityId) {
        return jpa.findByActivityIdOrderByNumberDesc(activityId);
    }
}
