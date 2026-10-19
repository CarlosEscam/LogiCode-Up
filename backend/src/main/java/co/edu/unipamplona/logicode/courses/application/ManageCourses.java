package co.edu.unipamplona.logicode.courses.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.domain.Course;
import co.edu.unipamplona.logicode.courses.domain.CourseEnrollment;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.UserDirectory;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crear, editar y consultar cursos. Crea cursos quien puede crear contenido (docente o privilegio completo) y queda
 * como docente responsable.
 * <p>
 * El primer curso que se crea inscribe a todas las cédulas ya habilitadas: así los estudiantes cargados en la
 * Etapa 3 no pierden acceso al contenido privado.
 */
@Service
public class ManageCourses {

    private final CourseRepository courses;
    private final CourseEnrollmentRepository enrollments;
    private final CourseAccess access;
    private final UserDirectory directory;
    private final Clock clock;

    public ManageCourses(CourseRepository courses, CourseEnrollmentRepository enrollments, CourseAccess access,
            UserDirectory directory, Clock clock) {
        this.courses = courses;
        this.enrollments = enrollments;
        this.access = access;
        this.directory = directory;
        this.clock = clock;
    }

    @Transactional
    public CourseView create(Actor actor, String name, String period, String description) {
        actor.require(Permission.MANAGE_COURSE_CONTENT);
        Instant now = clock.instant();
        boolean first = courses.count() == 0;
        Course course = courses.save(new Course(required(name, "nombre"), required(period, "periodo"),
                optional(description), actor.userId(), now));
        if (first) {
            for (String cedula : directory.allEnabledCedulas()) {
                enrollments.save(new CourseEnrollment(course.getId(), cedula, actor.userId(), now));
            }
        }
        return view(actor, course);
    }

    @Transactional
    public CourseView update(Actor actor, UUID courseId, String name, String period, String description) {
        access.requireManageContent(actor, courseId);
        Course course = access.require(courseId);
        course.update(required(name, "nombre"), required(period, "periodo"), optional(description), clock.instant());
        return view(actor, courses.save(course));
    }

    @Transactional(readOnly = true)
    public CourseView get(Actor actor, UUID courseId) {
        return view(actor, access.requireViewableCourse(actor, courseId));
    }

    /** Cursos en los que el actor participa (o todos, para un Administrador), del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<CourseView> list(Actor actor) {
        return courses.findAllById(access.viewableCourseIds(actor)).stream()
                .sorted(Comparator.comparing(Course::getCreatedAt).reversed())
                .map(c -> view(actor, c))
                .toList();
    }

    private CourseView view(Actor actor, Course course) {
        return new CourseView(course.getId(), course.getName(), course.getPeriod(), course.getDescription(),
                directory.fullNameOf(course.getOwnerId()).orElse(null), enrollments.countByCourse(course.getId()),
                access.canManageContent(actor, course), access.canManageEnrollments(actor, course),
                course.getCreatedAt());
    }

    static String required(String value, String field) {
        String cleaned = optional(value);
        if (cleaned == null) {
            throw ApplicationException.businessRule("El " + field + " del curso es obligatorio.");
        }
        return cleaned;
    }

    static String optional(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
