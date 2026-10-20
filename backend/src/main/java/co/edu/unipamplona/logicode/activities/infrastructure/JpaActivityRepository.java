package co.edu.unipamplona.logicode.activities.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByCourseIdAndArchivedAtIsNullOrderByClosesAtAscCreatedAtAsc(UUID courseId);
}
