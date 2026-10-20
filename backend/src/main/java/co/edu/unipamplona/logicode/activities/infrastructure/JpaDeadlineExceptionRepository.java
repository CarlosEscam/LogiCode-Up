package co.edu.unipamplona.logicode.activities.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.DeadlineException;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaDeadlineExceptionRepository extends JpaRepository<DeadlineException, UUID> {

    Optional<DeadlineException> findByActivityIdAndStudentId(UUID activityId, UUID studentId);

    List<DeadlineException> findByActivityId(UUID activityId);

    List<DeadlineException> findByStudentIdAndActivityIdIn(UUID studentId, Collection<UUID> activityIds);
}
