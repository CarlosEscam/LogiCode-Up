package co.edu.unipamplona.logicode.identity.api;

import java.time.Instant;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.api.AuthDtos.UserSummary;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.ApproveTeacher;
import co.edu.unipamplona.logicode.identity.application.ChangeUserRole;
import co.edu.unipamplona.logicode.identity.application.ChangeUserStatus;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.identity.application.ListAuditEvents;
import co.edu.unipamplona.logicode.identity.application.ListUsers;
import co.edu.unipamplona.logicode.identity.domain.AccountStatus;
import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import co.edu.unipamplona.logicode.shared.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
@Tag(name = "Administración", description = "Usuarios, aprobación docente, roles y auditoría")
public class AdminUserController {

    public record RoleChangeRequest(@NotNull Role role) {
    }

    public record StatusChangeRequest(@NotNull AccountStatus status) {
    }

    public record AuditEventResponse(UUID id, AuditAction action, UUID actorId, UUID targetUserId, String subject,
            AuditEvent.Outcome outcome, String requestId, String details, Instant occurredAt) {

        static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(e.getId(), e.getAction(), e.getActorId(), e.getTargetUserId(), e.getSubject(),
                    e.getOutcome(), e.getRequestId(), e.getDetails(), e.getOccurredAt());
        }
    }

    private final ListUsers listUsers;
    private final ApproveTeacher approveTeacher;
    private final ChangeUserRole changeUserRole;
    private final ChangeUserStatus changeUserStatus;
    private final ListAuditEvents listAuditEvents;

    public AdminUserController(ListUsers listUsers, ApproveTeacher approveTeacher, ChangeUserRole changeUserRole,
            ChangeUserStatus changeUserStatus, ListAuditEvents listAuditEvents) {
        this.listUsers = listUsers;
        this.approveTeacher = approveTeacher;
        this.changeUserRole = changeUserRole;
        this.changeUserStatus = changeUserStatus;
        this.listAuditEvents = listAuditEvents;
    }

    @GetMapping("/users")
    @Operation(summary = "Listar cuentas con filtros por rol y estado")
    public PageResult<UserSummary> users(@CurrentActor Actor actor,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return listUsers.execute(actor, q, role, status, page, size).map(UserSummary::from);
    }

    @PostMapping("/teachers/{id}/approve")
    @Operation(summary = "Aprobar una solicitud de cuenta docente (RF-02)")
    public UserSummary approveTeacher(@CurrentActor Actor actor, @PathVariable UUID id) {
        return UserSummary.from(approveTeacher.execute(actor, id));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Cambiar el rol de una cuenta (solo privilegio completo, RN-04)")
    public UserSummary changeRole(@CurrentActor Actor actor, @PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody RoleChangeRequest request) {
        return UserSummary.from(changeUserRole.execute(actor, id, request.role()));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activar o desactivar una cuenta (RN-16 protege las cuentas iniciales)")
    public UserSummary changeStatus(@CurrentActor Actor actor, @PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody StatusChangeRequest request) {
        return UserSummary.from(changeUserStatus.execute(actor, id, request.status()));
    }

    @GetMapping("/audit-events")
    @Operation(summary = "Consultar la auditoría de acceso")
    public PageResult<AuditEventResponse> auditEvents(@CurrentActor Actor actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return listAuditEvents.execute(actor, page, size).map(AuditEventResponse::from);
    }
}
