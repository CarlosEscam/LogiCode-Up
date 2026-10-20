package co.edu.unipamplona.logicode.activities.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Activity;

public interface ActivityRepository {

    Activity save(Activity activity);

    Optional<Activity> findById(UUID id);

    /** Actividades no archivadas del curso, por fecha de cierre. */
    List<Activity> findActiveByCourse(UUID courseId);
}
