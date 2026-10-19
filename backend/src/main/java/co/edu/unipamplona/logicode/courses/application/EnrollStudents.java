package co.edu.unipamplona.logicode.courses.application;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.domain.Course;
import co.edu.unipamplona.logicode.courses.domain.CourseEnrollment;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.EnableStudents;
import co.edu.unipamplona.logicode.identity.application.StudentCsvReader;
import co.edu.unipamplona.logicode.identity.application.UserDirectory;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscripción de estudiantes en un curso por cédula (RF-05, RN-03). Inscribir también habilita la cédula, para que
 * el estudiante pueda registrarse.
 */
@Service
public class EnrollStudents {

    public record EnrolledStudent(String cedula, String fullName, boolean registered, Instant enrolledAt) {
    }

    public record ImportResult(int enrolled, int alreadyEnrolled, List<StudentCsvReader.RowError> errors) {
    }

    private final CourseEnrollmentRepository enrollments;
    private final CourseAccess access;
    private final EnableStudents enableStudents;
    private final UserDirectory directory;
    private final Clock clock;

    public EnrollStudents(CourseEnrollmentRepository enrollments, CourseAccess access, EnableStudents enableStudents,
            UserDirectory directory, Clock clock) {
        this.enrollments = enrollments;
        this.access = access;
        this.enableStudents = enableStudents;
        this.directory = directory;
        this.clock = clock;
    }

    @Transactional
    public EnrolledStudent enrollOne(Actor actor, UUID courseId, String rawCedula, String fullName) {
        Course course = requireManageEnrollments(actor, courseId);
        String cedula = enableStudents.ensureEnabled(actor, rawCedula, fullName);
        if (enrollments.exists(course.getId(), cedula)) {
            throw ApplicationException.conflict("Este estudiante ya está inscrito en el curso.");
        }
        CourseEnrollment saved = enrollments.save(new CourseEnrollment(course.getId(), cedula, actor.userId(),
                clock.instant()));
        return describe(List.of(saved)).getFirst();
    }

    @Transactional
    public ImportResult importCsv(Actor actor, UUID courseId, InputStream csv) {
        Course course = requireManageEnrollments(actor, courseId);
        StudentCsvReader.Result parsed = StudentCsvReader.read(csv);
        int enrolled = 0;
        int already = parsed.duplicates();
        for (StudentCsvReader.Row row : parsed.rows()) {
            String cedula = enableStudents.ensureEnabled(actor, row.cedula(), row.fullName());
            if (enrollments.exists(course.getId(), cedula)) {
                already++;
                continue;
            }
            enrollments.save(new CourseEnrollment(course.getId(), cedula, actor.userId(), clock.instant()));
            enrolled++;
        }
        return new ImportResult(enrolled, already, parsed.errors());
    }

    @Transactional(readOnly = true)
    public PageResult<EnrolledStudent> list(Actor actor, UUID courseId, int page, int size) {
        Course course = requireManageEnrollments(actor, courseId);
        PageResult<CourseEnrollment> result = enrollments.findByCourse(course.getId(), Math.max(page, 0),
                Math.clamp(size, 1, 100));
        return new PageResult<>(describe(result.items()), result.page(), result.size(), result.totalItems(),
                result.totalPages());
    }

    /** Retira al estudiante del curso. Su cédula sigue habilitada y su cuenta no cambia. */
    @Transactional
    public void unenroll(Actor actor, UUID courseId, String cedula) {
        Course course = requireManageEnrollments(actor, courseId);
        if (!enrollments.exists(course.getId(), cedula)) {
            throw ApplicationException.notFound("Este estudiante no está inscrito en el curso.");
        }
        enrollments.delete(course.getId(), cedula);
    }

    private Course requireManageEnrollments(Actor actor, UUID courseId) {
        Course course = access.requireViewableCourse(actor, courseId);
        if (!access.canManageEnrollments(actor, course)) {
            throw ApplicationException.forbidden("Solo el docente del curso o un Administrador puede inscribir estudiantes.");
        }
        return course;
    }

    private List<EnrolledStudent> describe(List<CourseEnrollment> items) {
        Map<String, UserDirectory.StudentInfo> info = directory.lookup(
                items.stream().map(CourseEnrollment::getCedula).toList());
        return items.stream().map(e -> {
            UserDirectory.StudentInfo student = info.get(e.getCedula());
            return new EnrolledStudent(e.getCedula(), student == null ? null : student.fullName(),
                    student != null && student.registered(), e.getEnrolledAt());
        }).toList();
    }
}
