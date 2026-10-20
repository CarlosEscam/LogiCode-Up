package co.edu.unipamplona.logicode.activities.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.SubmissionRepository;
import co.edu.unipamplona.logicode.activities.domain.Submission;
import org.springframework.stereotype.Repository;

@Repository
class SubmissionRepositoryAdapter implements SubmissionRepository {

    private final JpaSubmissionRepository jpa;

    SubmissionRepositoryAdapter(JpaSubmissionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Submission save(Submission submission) {
        return jpa.save(submission);
    }

    @Override
    public Optional<Submission> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Submission> findAllById(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllById(ids);
    }

    @Override
    public List<Submission> findByActivityAndStudent(UUID activityId, UUID studentId) {
        return jpa.findByActivityIdAndStudentIdOrderByAttemptAsc(activityId, studentId);
    }

    @Override
    public List<Submission> findByActivity(UUID activityId) {
        return jpa.findByActivityIdOrderBySubmittedAtAsc(activityId);
    }

    @Override
    public List<Submission> findByStudentAndActivities(UUID studentId, Collection<UUID> activityIds) {
        return activityIds.isEmpty() ? List.of()
                : jpa.findByStudentIdAndActivityIdInOrderByAttemptAsc(studentId, activityIds);
    }
}
