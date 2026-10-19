package co.edu.unipamplona.logicode.identity.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import co.edu.unipamplona.logicode.shared.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de la API de identidad. Las contraseñas nunca se devuelven ni aparecen en {@code toString()}. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegistrationRequest(
            @NotBlank @Size(max = 20) String cedula,
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 200) String password) {

        @Override
        public String toString() {
            return "RegistrationRequest[cedula=" + cedula + "]";
        }
    }

    public record LoginRequest(@NotBlank @Size(max = 20) String cedula, @NotBlank @Size(max = 200) String password) {

        @Override
        public String toString() {
            return "LoginRequest[cedula=" + cedula + "]";
        }
    }

    public record TokenRequest(@NotBlank @Size(max = 100) String token) {

        @Override
        public String toString() {
            return "TokenRequest[***]";
        }
    }

    public record PasswordRecoveryRequest(@NotBlank @Size(max = 254) String email) {
    }

    public record PasswordResetRequest(@NotBlank @Size(max = 100) String token,
            @NotBlank @Size(max = 200) String newPassword) {

        @Override
        public String toString() {
            return "PasswordResetRequest[***]";
        }
    }

    public record PasswordChangeRequest(@NotBlank @Size(max = 200) String currentPassword,
            @NotBlank @Size(max = 200) String newPassword) {

        @Override
        public String toString() {
            return "PasswordChangeRequest[***]";
        }
    }

    public record EmailChangeRequest(
            @NotBlank @Size(max = 20) String cedula,
            @NotBlank @Size(max = 254) String lastEmail,
            @NotBlank @Size(max = 200) String password,
            @NotBlank @Size(max = 254) String newEmail) {

        @Override
        public String toString() {
            return "EmailChangeRequest[cedula=" + cedula + "]";
        }
    }

    public record MessageResponse(String message) {
    }

    public record CsrfResponse(String headerName, String token) {
    }

    public record EmailConfirmedResponse(String email, boolean passwordSetupRequired, String message) {
    }

    /** Cuenta propia: datos y permisos efectivos calculados por el backend. */
    public record MeResponse(UUID id, String cedula, String fullName, Role role, boolean fullPrivileges,
            String email, boolean emailVerified, Set<Permission> permissions) {

        static MeResponse from(UserAccount account, Actor actor) {
            return new MeResponse(account.getId(), account.getCedula(), account.getFullName(), account.getRole(),
                    account.isFullPrivileges(), account.getEmail(), account.isEmailVerified(), actor.permissions());
        }
    }

    public record AccountCreatedResponse(UUID id, String cedula, String fullName, Role role, AccountStatus status,
            String message) {
    }

    public record UserSummary(UUID id, String cedula, String fullName, Role role, AccountStatus status,
            boolean fullPrivileges, String email, boolean emailVerified, Instant createdAt) {

        static UserSummary from(UserAccount a) {
            return new UserSummary(a.getId(), a.getCedula(), a.getFullName(), a.getRole(), a.getStatus(),
                    a.isFullPrivileges(), a.getEmail(), a.isEmailVerified(), a.getCreatedAt());
        }
    }
}
