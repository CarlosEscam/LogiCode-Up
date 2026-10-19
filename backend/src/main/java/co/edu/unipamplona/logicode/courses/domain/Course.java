package co.edu.unipamplona.logicode.courses.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Curso de un periodo académico con su docente responsable. La primera versión trabaja con un curso y un docente;
 * el modelo admite más cursos y periodos.
 */
@Entity
@Table(name = "course")
public class Course {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String period;

    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Course() {
    }

    public Course(String name, String period, String description, UUID ownerId, Instant now) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.period = period;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String period, String description, Instant now) {
        this.name = name;
        this.period = period;
        this.description = description;
        this.updatedAt = now;
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPeriod() {
        return period;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
