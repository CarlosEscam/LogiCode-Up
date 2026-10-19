package co.edu.unipamplona.logicode.identity.api;

import co.edu.unipamplona.logicode.identity.api.AuthDtos.MeResponse;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.MessageResponse;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.PasswordChangeRequest;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.ChangePassword;
import co.edu.unipamplona.logicode.identity.application.GetCurrentActor;
import co.edu.unipamplona.logicode.identity.application.ResendEmailVerification;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/me")
@Tag(name = "Mi cuenta", description = "Datos, permisos y seguridad de la cuenta en sesión")
public class MeController {

    private final GetCurrentActor getCurrentActor;
    private final ChangePassword changePassword;
    private final ResendEmailVerification resendEmailVerification;

    public MeController(GetCurrentActor getCurrentActor, ChangePassword changePassword,
            ResendEmailVerification resendEmailVerification) {
        this.getCurrentActor = getCurrentActor;
        this.changePassword = changePassword;
        this.resendEmailVerification = resendEmailVerification;
    }

    @GetMapping
    @Operation(summary = "Consultar la cuenta en sesión y sus permisos (401 si no hay sesión)")
    public MeResponse me(@CurrentActor Actor actor) {
        return getCurrentActor.account(actor.userId())
                .map(account -> MeResponse.from(account, actor))
                .orElseThrow(() -> ApplicationException.unauthenticated("Debes iniciar sesión."));
    }

    @PostMapping("/password")
    @Operation(summary = "Cambiar la contraseña; cierra todas las sesiones, incluida la actual")
    public MessageResponse changePassword(@CurrentActor Actor actor, @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        changePassword.execute(actor, request.currentPassword(), request.newPassword());
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return new MessageResponse("Contraseña actualizada. Se cerraron todas tus sesiones; inicia sesión de nuevo.");
    }

    @PostMapping("/email-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Reenviar el enlace de verificación del correo actual")
    public MessageResponse resendVerification(@CurrentActor Actor actor) {
        resendEmailVerification.execute(actor);
        return new MessageResponse("Te enviamos un nuevo enlace de confirmación.");
    }
}
