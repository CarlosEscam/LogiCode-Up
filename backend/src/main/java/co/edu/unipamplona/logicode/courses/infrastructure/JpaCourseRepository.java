package co.edu.unipamplona.logicode.courses.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByOwnerId(UUID ownerId);
}
