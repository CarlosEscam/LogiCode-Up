package co.edu.unipamplona.logicode.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.edu.unipamplona.logicode.identity.application.EmailMessage;
import co.edu.unipamplona.logicode.identity.application.EmailSender;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas de aceptación de la Etapa 3 contra PostgreSQL real: registro, sesión, verificación de correo,
 * recuperación, cambio asistido de correo, permisos del administrador ordinario y protección RN-16.
 */
// RANDOM_PORT: la configuración de la cookie de Spring Session solo aplica con servidor embebido.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "logicode.identity.initial-admins[0].cedula=900001",
        "logicode.identity.initial-admins[0].full-name=Administradora Uno",
        "logicode.identity.initial-admins[0].email=admin1@example.com",
        "logicode.identity.initial-admins[1].cedula=900002",
        "logicode.identity.initial-admins[1].full-name=Administrador Dos",
        "logicode.identity.initial-admins[1].email=admin2@example.com"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class IdentityFlowsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    static final List<EmailMessage> OUTBOX = new CopyOnWriteArrayList<>();
    static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    static final String PASSWORD = "Clave#2026";

    static final MovableClock CLOCK = new MovableClock(Instant.now().truncatedTo(ChronoUnit.SECONDS));

    @TestConfiguration
    static class CapturingMail {
        @Bean
        @Primary
        EmailSender capturingEmailSender() {
            return OUTBOX::add;
        }

        @Bean
        @Primary
        Clock movableClock() {
            return CLOCK;
        }
    }

    static final class MovableClock extends Clock {

        private volatile Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Bogota");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Autowired
    MockMvc mvc;

    static Cookie admin1Session;

    @BeforeEach
    void ensureFullAdminSession() throws Exception {
        if (admin1Session == null) {
            admin1Session = activateInitialAdmin("900001", "admin1@example.com");
        }
    }

    @Test
    void initialAdminsAreCreatedWithFullPrivilegesAndNoPassword() throws Exception {
        mvc.perform(get("/api/v1/me").cookie(admin1Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.fullPrivileges").value(true))
                .andExpect(jsonPath("$.permissions.length()").value(14));
    }

    @Test
    void teacherRequestStudentEnablementAndRegistration() throws Exception {
        register("teacher-requests", "500001", "Docente Prueba", "docente@example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        login("500001", PASSWORD).andExpect(status().isForbidden());

        String teacherId = userId("500001");
        mvc.perform(get("/api/v1/admin/users").param("q", "DOCENTE prue").cookie(admin1Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cedula").value("500001"));
        mvc.perform(post("/api/v1/admin/teachers/" + teacherId + "/approve").cookie(admin1Session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        Cookie teacher = sessionOf(login("500001", PASSWORD).andExpect(status().isOk()).andReturn());

        // Sin cédula habilitada no hay registro (RN-01).
        register("student-registrations", "600001", "Estudiante Uno", "est1@example.com")
                .andExpect(status().isUnprocessableContent());

        mvc.perform(post("/api/v1/student-enablements").cookie(teacher).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cedula\":\"600001\"}"))
                .andExpect(status().isCreated());
        MockMultipartFile csv = new MockMultipartFile("file", "lista.csv", "text/csv",
                "cedula;nombre\n600002;Estudiante Dos\n600001;Repetida\nabc;Inválida\n".getBytes());
        mvc.perform(multipart("/api/v1/student-enablements/import").file(csv).cookie(teacher).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(1))
                .andExpect(jsonPath("$.alreadyEnabled").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1));

        register("student-registrations", "600001", "Estudiante Uno", "est1@example.com")
                .andExpect(status().isCreated());
        register("student-registrations", "600001", "Otra Persona", "otra@example.com")
                .andExpect(status().isConflict());
        Cookie student = sessionOf(login("600001", PASSWORD).andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/v1/me").cookie(student))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.emailVerified").value(false));

        // Un estudiante no puede habilitar cédulas ni aprobar docentes.
        mvc.perform(post("/api/v1/student-enablements").cookie(student).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cedula\":\"600099\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ordinaryAdminCannotChangeRolesAndOnlyInitialAccountsCan() throws Exception {
        createActiveStudent("610001", "ordinario@example.com");
        String id = userId("610001");
        mvc.perform(patch("/api/v1/admin/users/" + id + "/role").cookie(admin1Session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.fullPrivileges").value(false));

        Cookie ordinary = sessionOf(login("610001", PASSWORD).andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/v1/me").cookie(ordinary))
                .andExpect(jsonPath("$.permissions[?(@ == 'CHANGE_ROLES')]").isEmpty())
                .andExpect(jsonPath("$.permissions[?(@ == 'GRADE')]").isEmpty())
                .andExpect(jsonPath("$.permissions[?(@ == 'MANAGE_COURSE_CONTENT')]").isEmpty())
                .andExpect(jsonPath("$.permissions[?(@ == 'ANSWER_ACTIVITIES')]").isEmpty());
        mvc.perform(patch("/api/v1/admin/users/" + id + "/role").cookie(ordinary).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"STUDENT\"}"))
                .andExpect(status().isForbidden());
        // Tampoco puede desactivar una cuenta con privilegio completo.
        mvc.perform(patch("/api/v1/admin/users/" + userId("900001") + "/status").cookie(ordinary).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isForbidden());
        // Sí puede consultar usuarios (gestión ordinaria).
        mvc.perform(get("/api/v1/admin/users").cookie(ordinary)).andExpect(status().isOk());
    }

    @Test
    void platformNeverLosesItsLastActiveFullAdmin() throws Exception {
        String admin1 = userId("900001");
        String admin2 = userId("900002");
        mvc.perform(patch("/api/v1/admin/users/" + admin2 + "/status").cookie(admin1Session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/admin/users/" + admin1 + "/role").cookie(admin1Session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TEACHER\"}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(patch("/api/v1/admin/users/" + admin1 + "/status").cookie(admin1Session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(patch("/api/v1/admin/users/" + admin2 + "/status").cookie(admin1Session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changingPasswordClosesEverySession() throws Exception {
        createActiveStudent("620001", "sesiones@example.com");
        Cookie first = sessionOf(login("620001", PASSWORD).andReturn());
        Cookie second = sessionOf(login("620001", PASSWORD).andReturn());
        mvc.perform(get("/api/v1/me").cookie(second)).andExpect(status().isOk());

        mvc.perform(post("/api/v1/me/password").cookie(first).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Nueva#2026\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/me").cookie(second)).andExpect(status().isUnauthorized());
        login("620001", PASSWORD).andExpect(status().isUnauthorized());
        login("620001", "Nueva#2026").andExpect(status().isOk());
    }

    @Test
    void passwordRecoveryNeedsVerifiedEmailIsSingleUseAndLimitedToTwoPerDay() throws Exception {
        createActiveStudent("630001", "recupera@example.com");
        // Correo sin verificar: respuesta genérica y ningún envío.
        int before = OUTBOX.size();
        recover("recupera@example.com").andExpect(status().isAccepted());
        assertThat(OUTBOX).hasSize(before);

        Cookie student = sessionOf(login("630001", PASSWORD).andReturn());
        mvc.perform(post("/api/v1/me/email-verification").cookie(student).with(csrf()))
                .andExpect(status().isAccepted());
        confirmEmail(lastTokenFor("recupera@example.com"));

        recover("recupera@example.com").andExpect(status().isAccepted());
        String firstToken = lastTokenFor("recupera@example.com");
        recover("recupera@example.com").andExpect(status().isAccepted());
        String secondToken = lastTokenFor("recupera@example.com");
        int afterTwo = OUTBOX.size();
        recover("recupera@example.com").andExpect(status().isAccepted());
        assertThat(OUTBOX).hasSize(afterTwo); // tercera solicitud del día: no se envía

        // Solo el último enlace sirve, y una sola vez.
        reset(firstToken, "Otra#2026").andExpect(status().isUnprocessableContent());
        reset(secondToken, "Otra#2026").andExpect(status().isOk());
        reset(secondToken, "Otra#2027").andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/v1/me").cookie(student)).andExpect(status().isUnauthorized());
        login("630001", "Otra#2026").andExpect(status().isOk());

        recover("nadie@example.com").andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(AuthController.GENERIC_RECOVERY_MESSAGE));
    }

    @Test
    void assistedEmailChangeAcceptsCurrentOrPreviousPasswordAndNeedsConfirmation() throws Exception {
        createActiveStudent("640001", "perdido@example.com");
        Cookie session = sessionOf(login("640001", PASSWORD).andReturn());
        mvc.perform(post("/api/v1/me/password").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Actual#2026\"}"));

        int before = OUTBOX.size();
        emailChange("640001", "perdido@example.com", "Equivocada#1", "nuevo@example.com")
                .andExpect(status().isAccepted());
        assertThat(OUTBOX).hasSize(before);

        // Contraseña inmediatamente anterior: válida (RN-25).
        emailChange("640001", "perdido@example.com", PASSWORD, "nuevo@example.com").andExpect(status().isAccepted());
        // P-02: el correo actual recibe un aviso.
        assertThat(OUTBOX.subList(before, OUTBOX.size())).extracting(EmailMessage::toAddress)
                .containsExactlyInAnyOrder("nuevo@example.com", "perdido@example.com");
        String token = lastTokenFor("nuevo@example.com");

        // Límite independiente de 2 intentos diarios para este flujo (ya se usaron 2).
        before = OUTBOX.size();
        emailChange("640001", "perdido@example.com", "Actual#2026", "otro@example.com")
                .andExpect(status().isAccepted());
        assertThat(OUTBOX).hasSize(before);

        // P-02: el enlace no sirve hasta 24 horas después; mientras tanto el correo no cambia.
        Cookie beforeConfirm = sessionOf(login("640001", "Actual#2026").andReturn());
        confirmEmailRaw(token).andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/v1/me").cookie(beforeConfirm)).andExpect(jsonPath("$.email").value("perdido@example.com"));

        CLOCK.advance(Duration.ofHours(25));
        confirmEmail(token);
        // P-02: confirmar el correo nuevo cierra las sesiones abiertas.
        mvc.perform(get("/api/v1/me").cookie(beforeConfirm)).andExpect(status().isUnauthorized());
        Cookie after = sessionOf(login("640001", "Actual#2026").andReturn());
        mvc.perform(get("/api/v1/me").cookie(after))
                .andExpect(jsonPath("$.email").value("nuevo@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void changingThePasswordCancelsAPendingEmailChange() throws Exception {
        createActiveStudent("641001", "duena@example.com");
        emailChange("641001", "duena@example.com", PASSWORD, "intruso@example.com").andExpect(status().isAccepted());
        String token = lastTokenFor("intruso@example.com");

        Cookie owner = sessionOf(login("641001", PASSWORD).andReturn());
        mvc.perform(post("/api/v1/me/password").cookie(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Propia#2026\"}"))
                .andExpect(status().isOk());

        CLOCK.advance(Duration.ofHours(25));
        confirmEmailRaw(token).andExpect(status().isUnprocessableContent());
    }

    @Test
    void registrationNeedsTheEnabledNameAndTeacherRequestsCannotTakeStudentCedulas() throws Exception {
        mvc.perform(post("/api/v1/student-enablements").cookie(admin1Session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"650001\",\"fullName\":\"María José Pérez\"}"))
                .andExpect(status().isCreated());

        // P-01: con la cédula ajena no basta.
        register("teacher-requests", "650001", "Impostor Docente", "impostor1@example.com")
                .andExpect(status().isConflict());
        register("student-registrations", "650001", "Impostor Total", "impostor2@example.com")
                .andExpect(status().isUnprocessableContent());
        // Sin tildes, en otro orden de mayúsculas y con el segundo apellido: es la misma persona.
        register("student-registrations", "650001", "maria jose PEREZ Gomez", "maria@example.com")
                .andExpect(status().isCreated());
    }

    @Test
    void repeatedLoginFailuresAreThrottled() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("700001", "Mala#1234").andExpect(status().isUnauthorized());
        }
        login("700001", "Mala#1234").andExpect(status().isTooManyRequests());
    }

    @Test
    void othersCannotLockOwnerOutOfAKnownDevice() throws Exception {
        createActiveStudent("710001", "conocido@example.com");
        Cookie device = login("710001", PASSWORD).andExpect(status().isOk()).andReturn().getResponse()
                .getCookie("LOGICODE_DEVICE");
        assertThat(device).as("cookie de equipo conocido").isNotNull();
        assertThat(device.isHttpOnly()).isTrue();

        for (int i = 0; i < 5; i++) {
            login("710001", "Mala#1234").andExpect(status().isUnauthorized());
        }
        // P-03: el atacante queda bloqueado; la dueña, desde su equipo, entra.
        login("710001", PASSWORD).andExpect(status().isTooManyRequests());
        mvc.perform(json(post("/api/v1/auth/login"), "{\"cedula\":\"710001\",\"password\":\"" + PASSWORD + "\"}")
                .cookie(device)).andExpect(status().isOk());
        // Una cookie inventada no cuenta como equipo conocido.
        mvc.perform(json(post("/api/v1/auth/login"), "{\"cedula\":\"710001\",\"password\":\"" + PASSWORD + "\"}")
                .cookie(new Cookie("LOGICODE_DEVICE", device.getValue() + "x"))).andExpect(status().isTooManyRequests());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"900001\",\"password\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    // ---------- utilidades ----------

    private Cookie activateInitialAdmin(String cedula, String email) throws Exception {
        confirmEmail(lastTokenFor(email))
                .andExpect(jsonPath("$.passwordSetupRequired").value(true));
        recover(email).andExpect(status().isAccepted());
        reset(lastTokenFor(email), PASSWORD).andExpect(status().isOk());
        return sessionOf(login(cedula, PASSWORD).andExpect(status().isOk()).andReturn());
    }

    private void createActiveStudent(String cedula, String email) throws Exception {
        mvc.perform(post("/api/v1/student-enablements").cookie(admin1Session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"cedula\":\"" + cedula + "\"}"))
                .andExpect(status().isCreated());
        register("student-registrations", cedula, "Estudiante " + cedula, email).andExpect(status().isCreated());
    }

    private String userId(String cedula) throws Exception {
        String body = mvc.perform(get("/api/v1/admin/users").param("size", "100").cookie(admin1Session))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.items[?(@.cedula == '" + cedula + "')].id");
        assertThat(ids).hasSize(1);
        return ids.getFirst();
    }

    private org.springframework.test.web.servlet.ResultActions register(String path, String cedula, String name,
            String email) throws Exception {
        return mvc.perform(json(post("/api/v1/auth/" + path), "{\"cedula\":\"" + cedula + "\",\"fullName\":\"" + name
                + "\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String cedula, String password) throws Exception {
        return mvc.perform(json(post("/api/v1/auth/login"),
                "{\"cedula\":\"" + cedula + "\",\"password\":\"" + password + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions recover(String email) throws Exception {
        return mvc.perform(json(post("/api/v1/auth/password-recovery"), "{\"email\":\"" + email + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions reset(String token, String password) throws Exception {
        return mvc.perform(json(post("/api/v1/auth/password-reset"),
                "{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions confirmEmail(String token) throws Exception {
        return confirmEmailRaw(token).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions confirmEmailRaw(String token) throws Exception {
        return mvc.perform(json(post("/api/v1/auth/email-verifications/confirm"), "{\"token\":\"" + token + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions emailChange(String cedula, String lastEmail,
            String password, String newEmail) throws Exception {
        return mvc.perform(json(post("/api/v1/auth/email-change-requests"), "{\"cedula\":\"" + cedula
                + "\",\"lastEmail\":\"" + lastEmail + "\",\"password\":\"" + password + "\",\"newEmail\":\"" + newEmail
                + "\"}"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static Cookie sessionOf(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("LOGICODE_SESSION");
        assertThat(cookie).as("cookie de sesión").isNotNull();
        assertThat(cookie.isHttpOnly()).as("cookie HttpOnly").isTrue();
        return cookie;
    }

    private static String lastTokenFor(String email) {
        EmailMessage message = OUTBOX.reversed().stream().filter(m -> m.toAddress().equals(email)).findFirst()
                .orElseThrow(() -> new AssertionError("No hay correo para " + email));
        Matcher matcher = TOKEN.matcher(message.textBody());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
