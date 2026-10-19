package co.edu.unipamplona.logicode.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Cuenta de usuario. Guarda solo los hashes de la contraseña actual y de la inmediatamente anterior (RNF-02, RN-25).
 * <p>
 * Las anotaciones JPA son metadatos de mapeo estándar (Jakarta Persistence); el dominio no depende de Spring.
 */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String cedula;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "full_privileges", nullable = false)
    private boolean fullPrivileges;

    @Column(nullable = false)
    private String email;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "previous_password_hash")
    private String previousPasswordHash;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected UserAccount() {
        // Requerido por JPA.
    }

    private UserAccount(String cedula, String fullName, String email, Role role, AccountStatus status,
            String passwordHash, Instant now) {
        this.id = UUID.randomUUID();
        this.cedula = Objects.requireNonNull(cedula);
        this.fullName = Objects.requireNonNull(fullName);
        this.email = Objects.requireNonNull(email);
        this.role = role;
        this.status = status;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordHash != null ? now : null;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Estudiante con cédula habilitada: queda activo de inmediato (RF-01, RN-01). */
    public static UserAccount registerStudent(String cedula, String fullName, String email, String passwordHash, Instant now) {
        return new UserAccount(cedula, fullName, email, Role.STUDENT, AccountStatus.ACTIVE, passwordHash, now);
    }

    /** Solicitud de cuenta docente: sin permisos hasta la aprobación (RF-02, RN-02). */
    public static UserAccount requestTeacher(String cedula, String fullName, String email, String passwordHash, Instant now) {
        return new UserAccount(cedula, fullName, email, Role.TEACHER, AccountStatus.PENDING_APPROVAL, passwordHash, now);
    }

    /** Cuenta inicial con privilegio completo (RN-22). No tiene contraseña hasta que la titular la defina. */
    public static UserAccount initialAdmin(String cedula, String fullName, String email, Instant now) {
        UserAccount account = new UserAccount(cedula, fullName, email, Role.ADMIN, AccountStatus.ACTIVE, null, now);
        account.fullPrivileges = true;
        return account;
    }

    public Set<Permission> permissions() {
        return AccessPolicy.permissionsFor(role, status, fullPrivileges);
    }

    public boolean hasPermission(Permission permission) {
        return permissions().contains(permission);
    }

    /** Rota los hashes: el actual pasa a anterior y se descarta el más antiguo (RNF-02, RN-25). */
    public void changePassword(String newPasswordHash, Instant now) {
        this.previousPasswordHash = this.passwordHash;
        this.passwordHash = Objects.requireNonNull(newPasswordHash);
        this.passwordChangedAt = now;
        touch(now);
    }

    public void markEmailVerified(Instant now) {
        this.emailVerifiedAt = now;
        touch(now);
    }

    /** Reemplaza el correo por uno ya confirmado mediante su enlace (RN-25). */
    public void replaceEmailWithVerified(String newEmail, Instant now) {
        this.email = Objects.requireNonNull(newEmail);
        this.emailVerifiedAt = now;
        touch(now);
    }

    public void approveAsTeacher(Instant now) {
        if (role != Role.TEACHER || status != AccountStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("La cuenta no es una solicitud docente pendiente");
        }
        this.status = AccountStatus.ACTIVE;
        touch(now);
    }

    /** Cambia el rol. Una cuenta que deja de ser Administrador pierde el privilegio completo. */
    public void changeRole(Role newRole, Instant now) {
        this.role = Objects.requireNonNull(newRole);
        if (newRole != Role.ADMIN) {
            this.fullPrivileges = false;
        }
        touch(now);
    }

    public void changeStatus(AccountStatus newStatus, Instant now) {
        this.status = Objects.requireNonNull(newStatus);
        touch(now);
    }

    /** Asegura que una cuenta inicial conserve rol Administrador, privilegio completo y estado activo. */
    public void ensureInitialAdmin(Instant now) {
        this.role = Role.ADMIN;
        this.fullPrivileges = true;
        this.status = AccountStatus.ACTIVE;
        touch(now);
    }

    public void revokeFullPrivileges(Instant now) {
        this.fullPrivileges = false;
        touch(now);
    }

    public boolean isActiveFullAdmin() {
        return role == Role.ADMIN && fullPrivileges && status == AccountStatus.ACTIVE;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    private void touch(Instant now) {
        this.updatedAt = now;
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

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isFullPrivileges() {
        return fullPrivileges;
    }

    public String getEmail() {
        return email;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPreviousPasswordHash() {
        return previousPasswordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
