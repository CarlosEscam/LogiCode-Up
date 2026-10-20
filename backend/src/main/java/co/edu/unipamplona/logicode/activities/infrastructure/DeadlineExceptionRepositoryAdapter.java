package co.edu.unipamplona.logicode.activities.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.DeadlineExceptionRepository;
import co.edu.unipamplona.logicode.activities.domain.DeadlineException;
import org.springframework.stereotype.Repository;

@Repository
class DeadlineExceptionRepositoryAdapter implements DeadlineExceptionRepository {

    private final JpaDeadlineExceptionRepository jpa;

    DeadlineExceptionRepositoryAdapter(JpaDeadlineExceptionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DeadlineException save(DeadlineException exception) {
        return jpa.save(exception);
    }

    @Override
    public Optional<DeadlineException> find(UUID activityId, UUID studentId) {
        return jpa.findByActivityIdAndStudentId(activityId, studentId);
    }

    @Override
    public List<DeadlineException> findByActivity(UUID activityId) {
        return jpa.findByActivityId(activityId);
    }

    @Override
    public List<DeadlineException> findByStudentAndActivities(UUID studentId, Collection<UUID> activityIds) {
        return activityIds.isEmpty() ? List.of() : jpa.findByStudentIdAndActivityIdIn(studentId, activityIds);
    }

    @Override
    public void delete(DeadlineException exception) {
        jpa.delete(exception);
    }
}
