package co.edu.unipamplona.logicode.courses.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.domain.CourseEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface JpaCourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {

    boolean existsByCourseIdAndCedula(UUID courseId, String cedula);

    @Query("select e.courseId from CourseEnrollment e where e.cedula = :cedula")
    List<UUID> findCourseIdsByCedula(String cedula);

    Page<CourseEnrollment> findByCourseId(UUID courseId, Pageable pageable);

    long countByCourseId(UUID courseId);

    @Query("select e.cedula from CourseEnrollment e where e.courseId = :courseId order by e.cedula")
    List<String> findCedulasByCourseId(UUID courseId);

    @Modifying
    @Query("delete from CourseEnrollment e where e.courseId = :courseId and e.cedula = :cedula")
    void deleteByCourseIdAndCedula(UUID courseId, String cedula);
}
