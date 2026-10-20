package co.edu.unipamplona.logicode.activities.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaSubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByActivityIdAndStudentIdOrderByAttemptAsc(UUID activityId, UUID studentId);

    List<Submission> findByActivityIdOrderBySubmittedAtAsc(UUID activityId);

    List<Submission> findByStudentIdAndActivityIdInOrderByAttemptAsc(UUID studentId, Collection<UUID> activityIds);
}
