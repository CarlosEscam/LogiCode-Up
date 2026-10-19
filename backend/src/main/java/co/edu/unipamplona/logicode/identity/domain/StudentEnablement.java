package co.edu.unipamplona.logicode.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cédula habilitada por un docente para que el estudiante pueda registrarse (RF-05, RN-01). */
@Entity
@Table(name = "student_enablement")
public class StudentEnablement {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String cedula;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "enabled_by", nullable = false, updatable = false)
    private UUID enabledBy;

    @Column(name = "enabled_at", nullable = false, updatable = false)
    private Instant enabledAt;

    @Column(name = "registered_user_id")
    private UUID registeredUserId;

    protected StudentEnablement() {
    }

    public StudentEnablement(String cedula, String fullName, UUID enabledBy, Instant now) {
        this.id = UUID.randomUUID();
        this.cedula = cedula;
        this.fullName = fullName;
        this.enabledBy = enabledBy;
        this.enabledAt = now;
    }

    public void markRegistered(UUID userId) {
        this.registeredUserId = userId;
    }

    public boolean isRegistered() {
        return registeredUserId != null;
    }

    public UUID getId() {
        return id;
    }

    public String getCedula() {
        return cedula;
    }

    public String getFullName() {
        return fullName;
    }

    public Instant getEnabledAt() {
        return enabledAt;
    }
}
