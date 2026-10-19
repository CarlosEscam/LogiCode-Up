package co.edu.unipamplona.logicode.courses.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Estudiante inscrito en un curso, identificado por cédula. Así la inscripción puede hacerse antes de que el
 * estudiante se registre; al registrarse ve el contenido privado del curso (RN-03).
 */
@Entity
@Table(name = "course_enrollment")
public class CourseEnrollment {

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false, updatable = false)
    private String cedula;

    @Column(name = "enrolled_by", updatable = false)
    private UUID enrolledBy;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    protected CourseEnrollment() {
    }

    public CourseEnrollment(UUID courseId, String cedula, UUID enrolledBy, Instant now) {
        this.id = UUID.randomUUID();
        this.courseId = courseId;
        this.cedula = cedula;
        this.enrolledBy = enrolledBy;
        this.enrolledAt = now;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getCedula() {
        return cedula;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
