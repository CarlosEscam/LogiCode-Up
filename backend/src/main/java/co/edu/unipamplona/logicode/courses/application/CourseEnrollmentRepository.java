package co.edu.unipamplona.logicode.courses.application;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.domain.CourseEnrollment;
import co.edu.unipamplona.logicode.shared.application.PageResult;

public interface CourseEnrollmentRepository {

    CourseEnrollment save(CourseEnrollment enrollment);

    boolean exists(UUID courseId, String cedula);

    List<UUID> courseIdsFor(String cedula);

    PageResult<CourseEnrollment> findByCourse(UUID courseId, int page, int size);

    long countByCourse(UUID courseId);

    void delete(UUID courseId, String cedula);
}
