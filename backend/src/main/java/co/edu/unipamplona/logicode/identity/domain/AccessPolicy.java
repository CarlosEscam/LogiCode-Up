package co.edu.unipamplona.logicode.identity.domain;

import static co.edu.unipamplona.logicode.shared.domain.Permission.*;

import java.util.EnumSet;
import java.util.Set;

import co.edu.unipamplona.logicode.shared.domain.Permission;
import co.edu.unipamplona.logicode.shared.domain.Role;

/**
 * Permisos efectivos según rol, estado y privilegio completo (RF-28, RN-04, RN-15, RN-22).
 * <p>
 * Las cuentas Administrador ordinarias no pueden responder actividades, crear temas/materiales/actividades,
 * calificar ni ajustar notas, ni cambiar roles. Solo las cuentas con privilegio completo tienen todos los permisos.
 */
public final class AccessPolicy {

    private static final Set<Permission> STUDENT = EnumSet.of(
            VIEW_PRIVATE_COURSE_CONTENT, PARTICIPATE_FORUM, ANSWER_ACTIVITIES);

    private static final Set<Permission> TEACHER = EnumSet.of(
            VIEW_PRIVATE_COURSE_CONTENT, PARTICIPATE_FORUM, MODERATE_FORUM, MANAGE_COURSE_CONTENT, GRADE,
            VIEW_COURSE_GRADES, MANAGE_STUDENT_ENABLEMENTS);

    private static final Set<Permission> ORDINARY_ADMIN = EnumSet.of(
            VIEW_PRIVATE_COURSE_CONTENT, PARTICIPATE_FORUM, MODERATE_FORUM, VIEW_COURSE_GRADES,
            MANAGE_STUDENT_ENABLEMENTS, APPROVE_TEACHERS, MANAGE_USERS, MANAGE_LIBRARY, MANAGE_BACKUPS, VIEW_AUDIT);

    private AccessPolicy() {
    }

    public static Set<Permission> permissionsFor(Role role, AccountStatus status, boolean fullPrivileges) {
        if (status != AccountStatus.ACTIVE) {
            return EnumSet.noneOf(Permission.class);
        }
        if (role == Role.ADMIN && fullPrivileges) {
            return EnumSet.allOf(Permission.class);
        }
        return switch (role) {
            case STUDENT -> EnumSet.copyOf(STUDENT);
            case TEACHER -> EnumSet.copyOf(TEACHER);
            case ADMIN -> EnumSet.copyOf(ORDINARY_ADMIN);
        };
    }
}
