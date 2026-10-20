package co.edu.unipamplona.logicode.courses.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.application.CourseEnrollmentRepository;
import co.edu.unipamplona.logicode.courses.domain.CourseEnrollment;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class CourseEnrollmentRepositoryAdapter implements CourseEnrollmentRepository {

    private final JpaCourseEnrollmentRepository jpa;

    CourseEnrollmentRepositoryAdapter(JpaCourseEnrollmentRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CourseEnrollment save(CourseEnrollment enrollment) {
        return jpa.save(enrollment);
    }

    @Override
    public boolean exists(UUID courseId, String cedula) {
        return cedula != null && jpa.existsByCourseIdAndCedula(courseId, cedula);
    }

    @Override
    public List<UUID> courseIdsFor(String cedula) {
        return cedula == null ? List.of() : jpa.findCourseIdsByCedula(cedula);
    }

    @Override
    public PageResult<CourseEnrollment> findByCourse(UUID courseId, int page, int size) {
        Page<CourseEnrollment> result = jpa.findByCourseId(courseId,
                PageRequest.of(page, size, Sort.by("enrolledAt").descending().and(Sort.by("cedula"))));
        return new PageResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public long countByCourse(UUID courseId) {
        return jpa.countByCourseId(courseId);
    }

    @Override
    public List<String> cedulasOf(UUID courseId) {
        return jpa.findCedulasByCourseId(courseId);
    }

    @Override
    public void delete(UUID courseId, String cedula) {
        jpa.deleteByCourseIdAndCedula(courseId, cedula);
    }
}
