package co.edu.unipamplona.logicode.courses.application;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.domain.Course;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import co.edu.unipamplona.logicode.shared.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de acceso a un curso (RN-03, RN-04), usadas también por el módulo de contenidos:
 * <ul>
 * <li>Administra el contenido: el docente responsable o una cuenta con privilegio completo.</li>
 * <li>Ve el contenido privado: los inscritos, el docente responsable y las cuentas Administrador.</li>
 * <li>Inscribe estudiantes: quien administra el curso o una cuenta Administrador.</li>
 * </ul>
 */
@Service
public class CourseAccess {

    public record CourseSummary(UUID id, String name, String period) {
    }

    private final CourseRepository courses;
    private final CourseEnrollmentRepository enrollments;

    public CourseAccess(CourseRepository courses, CourseEnrollmentRepository enrollments) {
        this.courses = courses;
        this.enrollments = enrollments;
    }

    @Transactional(readOnly = true)
    public CourseSummary summary(UUID courseId) {
        Course course = require(courseId);
        return new CourseSummary(course.getId(), course.getName(), course.getPeriod());
    }

    /** 404 si el curso no existe o el actor no puede ver su contenido privado, para no revelar cursos ajenos. */
    @Transactional(readOnly = true)
    public void requireViewable(Actor actor, UUID courseId) {
        requireViewableCourse(actor, courseId);
    }

    Course requireViewableCourse(Actor actor, UUID courseId) {
        Course course = require(courseId);
        if (!canViewPrivate(actor, course)) {
            throw ApplicationException.notFound("El curso no existe.");
        }
        return course;
    }

    @Transactional(readOnly = true)
    public void requireManageContent(Actor actor, UUID courseId) {
        Course course = requireViewableCourse(actor, courseId);
        if (!canManageContent(actor, course)) {
            throw ApplicationException.forbidden("Solo el docente del curso puede modificar su contenido.");
        }
    }

    @Transactional(readOnly = true)
    public boolean canManageContent(Actor actor, UUID courseId) {
        return courses.findById(courseId).map(c -> canManageContent(actor, c)).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canViewPrivate(Actor actor, UUID courseId) {
        return courses.findById(courseId).map(c -> canViewPrivate(actor, c)).orElse(false);
    }

    /** Cursos cuyo contenido privado puede ver el actor; vacío para un visitante. */
    @Transactional(readOnly = true)
    public Set<UUID> viewableCourseIds(Actor actor) {
        if (actor == null || !actor.can(Permission.VIEW_PRIVATE_COURSE_CONTENT)) {
            return Set.of();
        }
        if (isStaff(actor)) {
            Set<UUID> all = new HashSet<>();
            courses.findAll().forEach(c -> all.add(c.getId()));
            return all;
        }
        Set<UUID> ids = new HashSet<>(enrollments.courseIdsFor(actor.cedula()));
        courses.findByOwner(actor.userId()).forEach(c -> ids.add(c.getId()));
        return ids;
    }

    boolean canManageContent(Actor actor, Course course) {
        return actor != null && actor.can(Permission.MANAGE_COURSE_CONTENT)
                && (actor.fullPrivileges() || course.isOwnedBy(actor.userId()));
    }

    boolean canManageEnrollments(Actor actor, Course course) {
        return actor != null && actor.can(Permission.MANAGE_STUDENT_ENABLEMENTS)
                && (isStaff(actor) || course.isOwnedBy(actor.userId()));
    }

    boolean canViewPrivate(Actor actor, Course course) {
        if (actor == null || !actor.can(Permission.VIEW_PRIVATE_COURSE_CONTENT)) {
            return false;
        }
        return isStaff(actor) || course.isOwnedBy(actor.userId())
                || enrollments.exists(course.getId(), actor.cedula());
    }

    Course require(UUID courseId) {
        return courses.findById(courseId).orElseThrow(() -> ApplicationException.notFound("El curso no existe."));
    }

    private static boolean isStaff(Actor actor) {
        return actor.fullPrivileges() || actor.role() == Role.ADMIN;
    }
}
