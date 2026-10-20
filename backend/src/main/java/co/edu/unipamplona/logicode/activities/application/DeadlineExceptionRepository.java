package co.edu.unipamplona.logicode.activities.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.DeadlineException;

public interface DeadlineExceptionRepository {

    DeadlineException save(DeadlineException exception);

    Optional<DeadlineException> find(UUID activityId, UUID studentId);

    List<DeadlineException> findByActivity(UUID activityId);

    List<DeadlineException> findByStudentAndActivities(UUID studentId, Collection<UUID> activityIds);

    void delete(DeadlineException exception);
}
