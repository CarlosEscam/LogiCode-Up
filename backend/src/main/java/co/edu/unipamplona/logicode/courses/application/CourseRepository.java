package co.edu.unipamplona.logicode.courses.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.domain.Course;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(UUID id);

    List<Course> findAll();

    List<Course> findAllById(Collection<UUID> ids);

    List<Course> findByOwner(UUID ownerId);

    long count();
}
