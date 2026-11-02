package co.edu.unipamplona.logicode.activities.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaSubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByActivityIdAndStudentIdOrderByAttemptAsc(UUID activityId, UUID studentId);

    List<Submission> findByActivityIdOrderBySubmittedAtAsc(UUID activityId);

    List<Submission> findByStudentIdAndActivityIdInOrderByAttemptAsc(UUID studentId, Collection<UUID> activityIds);

    @Query("select f from Submission s join s.fileIds f where f in :fileIds")
    Set<UUID> fileIdsInUse(Collection<UUID> fileIds);
}
