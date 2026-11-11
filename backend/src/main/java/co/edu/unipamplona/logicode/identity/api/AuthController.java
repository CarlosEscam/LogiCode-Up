package co.edu.unipamplona.logicode.identity.api;

import java.net.URI;

import co.edu.unipamplona.logicode.identity.api.AuthDtos.AccountCreatedResponse;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.CsrfResponse;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.EmailChangeRequest;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.EmailConfirmedResponse;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.LoginRequest;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.MeResponse;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.MessageResponse;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.PasswordRecoveryRequest;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.PasswordResetRequest;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.RegistrationRequest;
import co.edu.unipamplona.logicode.identity.api.AuthDtos.TokenRequest;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.AuthenticateUser;
import co.edu.unipamplona.logicode.identity.application.ConfirmEmail;
import co.edu.unipamplona.logicode.identity.application.GetCurrentActor;
import co.edu.unipamplona.logicode.identity.application.RecordLogout;
import co.edu.unipamplona.logicode.identity.application.RegisterStudent;
import co.edu.unipamplona.logicode.identity.application.RequestEmailChange;
import co.edu.unipamplona.logicode.identity.application.RequestPasswordRecovery;
import co.edu.unipamplona.logicode.identity.application.RequestTeacherAccount;
import co.edu.unipamplona.logicode.identity.application.ResetPassword;
import co.edu.unipamplona.logicode.identity.application.TrustedDevices;
import co.edu.unipamplona.logicode.identity.domain.UserAccount;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
@Tag(name = "Autenticación", description = "Registro, sesión, verificación de correo y recuperación")
public class AuthController {

    static final String GENERIC_RECOVERY_MESSAGE =
            "Si el correo pertenece a una cuenta con correo verificado, recibirás un enlace para restablecer la contraseña.";
    static final String GENERIC_EMAIL_CHANGE_MESSAGE =
            "Si los datos coinciden, enviaremos un enlace al correo nuevo y un aviso al anterior. Por seguridad, el enlace "
                    + "sirve 24 horas después de pedirlo y el cambio se aplica al confirmarlo.";

    private final RegisterStudent registerStudent;
    private final RequestTeacherAccount requestTeacherAccount;
    private final AuthenticateUser authenticateUser;
    private final TrustedDevices trustedDevices;
    private final GetCurrentActor getCurrentActor;
    private final RecordLogout recordLogout;
    private final ConfirmEmail confirmEmail;
    private final RequestPasswordRecovery requestPasswordRecovery;
    private final ResetPassword resetPassword;
    private final RequestEmailChange requestEmailChange;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(RegisterStudent registerStudent, RequestTeacherAccount requestTeacherAccount,
            AuthenticateUser authenticateUser, GetCurrentActor getCurrentActor, RecordLogout recordLogout,
            ConfirmEmail confirmEmail, RequestPasswordRecovery requestPasswordRecovery, ResetPassword resetPassword,
            RequestEmailChange requestEmailChange, SecurityContextRepository securityContextRepository,
            TrustedDevices trustedDevices) {
        this.trustedDevices = trustedDevices;
        this.registerStudent = registerStudent;
        this.requestTeacherAccount = requestTeacherAccount;
        this.authenticateUser = authenticateUser;
        this.getCurrentActor = getCurrentActor;
        this.recordLogout = recordLogout;
        this.confirmEmail = confirmEmail;
        this.requestPasswordRecovery = requestPasswordRecovery;
        this.resetPassword = resetPassword;
        this.requestEmailChange = requestEmailChange;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtener el token CSRF (también queda en la cookie XSRF-TOKEN)")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/student-registrations")
    @Operation(summary = "Registrar un estudiante con cédula habilitada (RF-01)")
    public ResponseEntity<AccountCreatedResponse> registerStudent(@Valid @RequestBody RegistrationRequest request) {
        UserAccount account = registerStudent.execute(new RegisterStudent.Command(
                request.cedula(), request.fullName(), request.email(), request.password()));
        return created(account, "Cuenta creada. Revisa tu correo para confirmarlo.");
    }

    @PostMapping("/teacher-requests")
    @Operation(summary = "Solicitar una cuenta docente, pendiente de aprobación (RF-02)")
    public ResponseEntity<AccountCreatedResponse> requestTeacher(@Valid @RequestBody RegistrationRequest request) {
        UserAccount account = requestTeacherAccount.execute(new RequestTeacherAccount.Command(
                request.cedula(), request.fullName(), request.email(), request.password()));
        return created(account,
                "Solicitud enviada. Un administrador debe aprobarla; mientras tanto, confirma tu correo.");
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con cédula y contraseña (RF-03)")
    public MeResponse login(@Valid @RequestBody LoginRequest request,
            @CookieValue(name = TrustedDevices.COOKIE, required = false) String deviceCookie,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Actor actor = authenticateUser.execute(request.cedula(), request.password(), deviceCookie);
        // Equipo conocido (P-03): solo viaja al ingresar y JavaScript no la lee.
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(TrustedDevices.COOKIE,
                        trustedDevices.issue(actor.userId()))
                .httpOnly(true).secure(httpRequest.isSecure()).sameSite("Strict").path(ApiPaths.V1 + "/auth/login")
                .maxAge(TrustedDevices.VALIDITY).build().toString());

        HttpSession existing = httpRequest.getSession(false);
        if (existing != null) {
            httpRequest.changeSessionId();
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                actor.userId().toString(), null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        UserAccount account = getCurrentActor.account(actor.userId()).orElseThrow();
        return MeResponse.from(account, actor);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar la sesión actual")
    public void logout(HttpServletRequest request) {
        Object actor = request.getAttribute(CurrentActorArgumentResolver.ATTRIBUTE);
        if (actor instanceof Actor resolved) {
            recordLogout.execute(resolved);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @PostMapping("/email-verifications/confirm")
    @Operation(summary = "Confirmar un correo con el enlace de un solo uso (RF-29)")
    public EmailConfirmedResponse confirmEmail(@Valid @RequestBody TokenRequest request) {
        ConfirmEmail.Result result = confirmEmail.execute(request.token());
        String message = result.passwordSetupRequired()
                ? "Correo confirmado. Ahora define tu contraseña con la opción \"¿Olvidaste tu contraseña?\"."
                : "Correo confirmado.";
        return new EmailConfirmedResponse(result.email(), result.passwordSetupRequired(), message);
    }

    @PostMapping("/password-recovery")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicitar enlace de restablecimiento al correo verificado (RF-04)")
    public MessageResponse passwordRecovery(@Valid @RequestBody PasswordRecoveryRequest request) {
        requestPasswordRecovery.execute(request.email());
        return new MessageResponse(GENERIC_RECOVERY_MESSAGE);
    }

    @PostMapping("/password-reset")
    @Operation(summary = "Guardar una contraseña nueva con el enlace de recuperación (RF-04)")
    public MessageResponse passwordReset(@Valid @RequestBody PasswordResetRequest request) {
        resetPassword.execute(request.token(), request.newPassword());
        return new MessageResponse("Contraseña actualizada. Se cerraron todas tus sesiones; inicia sesión de nuevo.");
    }

    @PostMapping("/email-change-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Cambio asistido de correo si se perdió el acceso al vinculado (RF-30)")
    public MessageResponse emailChange(@Valid @RequestBody EmailChangeRequest request) {
        requestEmailChange.execute(new RequestEmailChange.Command(
                request.cedula(), request.lastEmail(), request.password(), request.newEmail()));
        return new MessageResponse(GENERIC_EMAIL_CHANGE_MESSAGE);
    }

    private static ResponseEntity<AccountCreatedResponse> created(UserAccount account, String message) {
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/me"))
                .body(new AccountCreatedResponse(account.getId(), account.getCedula(), account.getFullName(),
                        account.getRole(), account.getStatus(), message));
    }
}
