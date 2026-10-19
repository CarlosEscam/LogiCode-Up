package co.edu.unipamplona.logicode.courses.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.application.CourseRepository;
import co.edu.unipamplona.logicode.courses.domain.Course;
import org.springframework.stereotype.Repository;

@Repository
class CourseRepositoryAdapter implements CourseRepository {

    private final JpaCourseRepository jpa;

    CourseRepositoryAdapter(JpaCourseRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Course save(Course course) {
        return jpa.save(course);
    }

    @Override
    public Optional<Course> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Course> findAll() {
        return jpa.findAll();
    }

    @Override
    public List<Course> findAllById(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllById(ids);
    }

    @Override
    public List<Course> findByOwner(UUID ownerId) {
        return jpa.findByOwnerId(ownerId);
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
