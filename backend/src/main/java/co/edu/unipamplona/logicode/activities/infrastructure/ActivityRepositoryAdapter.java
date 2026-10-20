package co.edu.unipamplona.logicode.activities.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityRepository;
import co.edu.unipamplona.logicode.activities.domain.Activity;
import org.springframework.stereotype.Repository;

@Repository
class ActivityRepositoryAdapter implements ActivityRepository {

    private final JpaActivityRepository jpa;

    ActivityRepositoryAdapter(JpaActivityRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Activity save(Activity activity) {
        return jpa.save(activity);
    }

    @Override
    public Optional<Activity> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Activity> findActiveByCourse(UUID courseId) {
        return jpa.findByCourseIdAndArchivedAtIsNullOrderByClosesAtAscCreatedAtAsc(courseId);
    }
}
