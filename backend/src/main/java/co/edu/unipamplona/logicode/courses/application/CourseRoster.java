package co.edu.unipamplona.logicode.courses.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Estudiantes inscritos en un curso, para otros módulos (entregas, notas). No verifica permisos. */
@Service
public class CourseRoster {

    /** {@code userId} es nulo mientras el estudiante no se registre. */
    public record Member(String cedula, String fullName, UUID userId) {
    }

    private final CourseEnrollmentRepository enrollments;
    private final UserDirectory directory;

    public CourseRoster(CourseEnrollmentRepository enrollments, UserDirectory directory) {
        this.enrollments = enrollments;
        this.directory = directory;
    }

    /** Inscritos ordenados por nombre (los que aún no tienen nombre, al final por cédula). */
    @Transactional(readOnly = true)
    public List<Member> members(UUID courseId) {
        Map<String, UserDirectory.StudentInfo> info = directory.lookup(enrollments.cedulasOf(courseId));
        return info.values().stream()
                .map(s -> new Member(s.cedula(), s.fullName(), s.userId()))
                .sorted(Comparator.comparing((Member m) -> m.fullName() == null ? "￿" : m.fullName())
                        .thenComparing(Member::cedula))
                .toList();
    }
}
