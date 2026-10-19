package co.edu.unipamplona.logicode.shared.domain;

/**
 * Permisos de la plataforma. Ver la matriz en docs/permisos.md.
 */
public enum Permission {
    VIEW_PRIVATE_COURSE_CONTENT,
    PARTICIPATE_FORUM,
    MODERATE_FORUM,
    /** Responder actividades y quizzes. */
    ANSWER_ACTIVITIES,
    /** Crear temas, materiales y actividades. */
    MANAGE_COURSE_CONTENT,
    /** Calificar y ajustar notas. */
    GRADE,
    /** Consultar entregas y calificaciones del curso. */
    VIEW_COURSE_GRADES,
    /** Habilitar cédulas y cargar estudiantes. */
    MANAGE_STUDENT_ENABLEMENTS,
    APPROVE_TEACHERS,
    /** Gestionar usuarios (activar y desactivar cuentas). */
    MANAGE_USERS,
    MANAGE_LIBRARY,
    MANAGE_BACKUPS,
    VIEW_AUDIT,
    /** Cambiar el rol de cualquier cuenta: reservado al privilegio completo. */
    CHANGE_ROLES
}
