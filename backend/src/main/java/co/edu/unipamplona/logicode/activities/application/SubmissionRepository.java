package co.edu.unipamplona.logicode.activities.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Submission;

public interface SubmissionRepository {

    Submission save(Submission submission);

    Optional<Submission> findById(UUID id);

    List<Submission> findAllById(Collection<UUID> ids);

    /** Intentos del estudiante en la actividad, del primero al último. */
    List<Submission> findByActivityAndStudent(UUID activityId, UUID studentId);

    List<Submission> findByActivity(UUID activityId);

    /** Intentos del estudiante en varias actividades. */
    List<Submission> findByStudentAndActivities(UUID studentId, Collection<UUID> activityIds);
}
