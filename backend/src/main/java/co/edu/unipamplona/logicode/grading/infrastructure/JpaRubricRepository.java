package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaRubricRepository extends JpaRepository<RubricVersion, UUID> {

    Optional<RubricVersion> findFirstByActivityIdOrderByNumberDesc(UUID activityId);

    List<RubricVersion> findByActivityIdOrderByNumberDesc(UUID activityId);
}
