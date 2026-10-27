package co.edu.unipamplona.logicode.activities.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas de aceptación de la Etapa 5 contra PostgreSQL real. Punto de control: un ciclo completo de creación,
 * entrega y calificación funciona sin IA, y se respetan fechas y excepciones de plazo. El reloj del negocio se
 * adelanta para cruzar el cierre.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "logicode.identity.initial-admins[0].cedula=900001",
        "logicode.identity.initial-admins[0].full-name=Administradora Uno",
        "logicode.identity.initial-admins[0].email=admin1@example.com"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ActivityFlowsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    static final List<EmailMessage> OUTBOX = new CopyOnWriteArrayList<>();
    static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    static final String PASSWORD = "Clave#2026";
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    static final MovableClock CLOCK = new MovableClock(Instant.now().truncatedTo(ChronoUnit.SECONDS));

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        Path dir = Files.createTempDirectory("logicode-activities-it");
        registry.add("logicode.files.storage-dir", dir::toString);
    }

    @TestConfiguration
    static class TestBeans {
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

    /** Reloj que las pruebas pueden adelantar. */
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

    Cookie admin;
    Cookie teacher;
    Cookie student;
    Cookie classmate;
    Cookie outsider;
    Cookie ordinaryAdmin;
    Cookie otherTeacher;
    String courseId;
    String activityId;
    String firstSubmission;
    String studentUserId;
    String classmateUserId;
    Instant closesAt;

    @BeforeAll
    void createPeopleAndCourse() throws Exception {
        confirmEmail(lastTokenFor("admin1@example.com"));
        recover("admin1@example.com");
        reset(lastTokenFor("admin1@example.com"), PASSWORD);
        admin = login("900001");

        register("teacher-requests", "500001", "Docente Curso", "docente@example.com");
        mvc.perform(post("/api/v1/admin/teachers/" + userId("500001") + "/approve").cookie(admin).with(csrf()))
                .andExpect(status().isOk());
        teacher = login("500001");
        register("teacher-requests", "500002", "Docente Ajeno", "ajeno@example.com");
        mvc.perform(post("/api/v1/admin/teachers/" + userId("500002") + "/approve").cookie(admin).with(csrf()))
                .andExpect(status().isOk());
        otherTeacher = login("500002");

        courseId = JsonPath.read(mvc.perform(json(post("/api/v1/courses").cookie(teacher),
                        "{\"name\":\"Lógica de programación\",\"period\":\"2026-2\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        for (String cedula : List.of("600001", "600004")) {
            mvc.perform(json(post("/api/v1/courses/" + courseId + "/enrollments").cookie(teacher),
                            "{\"cedula\":\"" + cedula + "\"}"))
                    .andExpect(status().isCreated());
        }
        register("student-registrations", "600001", "Ana Estudiante", "ana@example.com");
        register("student-registrations", "600004", "Beto Estudiante", "beto@example.com");
        student = login("600001");
        classmate = login("600004");
        studentUserId = userId("600001");
        classmateUserId = userId("600004");

        for (String cedula : List.of("600002", "600005")) {
            mvc.perform(json(post("/api/v1/student-enablements").cookie(teacher), "{\"cedula\":\"" + cedula + "\"}"))
                    .andExpect(status().isCreated());
        }
        register("student-registrations", "600002", "Estudiante Externa", "externa@example.com");
        outsider = login("600002");
        register("student-registrations", "600005", "Admin Ordinario", "ordinario@example.com");
        mvc.perform(json(patch("/api/v1/admin/users/" + userId("600005") + "/role").cookie(admin),
                        "{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk());
        ordinaryAdmin = login("600005");
    }

    @Test
    @Order(1)
    void teacherCreatesDraftDefinesRubricAndPublishes() throws Exception {
        closesAt = CLOCK.instant().plus(Duration.ofDays(2));
        activityId = id(mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Algoritmo del promedio", "Escribe el pseudocódigo.", "60", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.fileGroups[0]").value("DOCUMENTS")));

        // El borrador no existe para el estudiante.
        mvc.perform(get("/api/v1/courses/" + courseId + "/activities").cookie(student))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/activities/" + activityId).cookie(student)).andExpect(status().isNotFound());

        // RN-20: sin rúbrica no se publica. RN-07: los porcentajes no superan 100 %.
        mvc.perform(post("/api/v1/activities/" + activityId + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Define la rúbrica de la actividad antes de publicarla."));
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Otra", "Otra más.", "50", 1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "Los porcentajes del curso sumarían 110 %. La suma no puede superar 100 % (disponible: 40 %)."));
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(ordinaryAdmin),
                        activity("No permitido", "No.", null, 1)))
                .andExpect(status().isForbidden());

        mvc.perform(json(put("/api/v1/activities/" + activityId + "/rubric").cookie(teacher), """
                        {"expectedAnswer":"Leer tres notas, sumarlas y dividir entre 3.",
                         "criteria":[{"name":"Entrada y salida","maxPoints":2},
                                     {"name":"Cálculo correcto","description":"Usa el promedio","maxPoints":3}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalPoints").value(5));
        mvc.perform(post("/api/v1/activities/" + activityId + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mvc.perform(get("/api/v1/activities/" + activityId).cookie(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.state").value("OPEN"))
                .andExpect(jsonPath("$.progress.canSubmit").value(true))
                .andExpect(jsonPath("$.canManage").value(false));
        mvc.perform(get("/api/v1/activities/" + activityId + "/rubric").cookie(student))
                .andExpect(jsonPath("$.criteria.length()").value(2))
                .andExpect(jsonPath("$.expectedAnswer").doesNotExist());
        mvc.perform(get("/api/v1/activities/" + activityId).cookie(outsider)).andExpect(status().isNotFound());
    }

    @Test
    @Order(2)
    void enrolledStudentSubmitsWithinAttempts() throws Exception {
        String url = "/api/v1/activities/" + activityId + "/submissions";
        mvc.perform(multipart(url).cookie(student).with(csrf()).param("text", "   "))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(multipart(url).file(new MockMultipartFile("files", "foto.png", "image/png", PNG))
                        .cookie(student).with(csrf()))
                .andExpect(status().isUnprocessableContent()); // no acepta fotos ni imágenes
        firstSubmission = id(mvc.perform(multipart(url)
                        .file(new MockMultipartFile("files", "solucion.pdf", "application/pdf", pdf()))
                        .param("text", "Leer n1, n2, n3; prom <- (n1+n2+n3)/3").cookie(student).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attempt").value(1))
                .andExpect(jsonPath("$.late").value(false))
                .andExpect(jsonPath("$.files[0].originalName").value("solucion.pdf")));

        mvc.perform(multipart(url).param("text", "otra vez").cookie(student).with(csrf()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Ya usaste los intentos permitidos para esta actividad."));
        mvc.perform(multipart(url).param("text", "intruso").cookie(outsider).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(multipart(url).param("text", "docente").cookie(teacher).with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/activities/" + activityId).cookie(student))
                .andExpect(jsonPath("$.progress.state").value("SUBMITTED"))
                .andExpect(jsonPath("$.progress.canSubmit").value(false));
        mvc.perform(get(url + "/mine").cookie(student)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(url).cookie(teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.cedula == '600001')].attempts").value(1))
                .andExpect(jsonPath("$[?(@.cedula == '600004')].progress.state").value("OPEN"));
        mvc.perform(get(url).cookie(student)).andExpect(status().isForbidden());

        String fileId = JsonPath.read(mvc.perform(get("/api/v1/submissions/" + firstSubmission).cookie(teacher))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.files[0].id");
        mvc.perform(get("/api/v1/submissions/" + firstSubmission + "/files/" + fileId + "/preview").cookie(student))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"));
        mvc.perform(get("/api/v1/submissions/" + firstSubmission).cookie(classmate)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/submissions/" + firstSubmission + "/files/" + fileId + "/download")
                        .cookie(classmate))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    void teacherGradesFromReviewQueueAndHistoryIsKept() throws Exception {
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(teacher))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].submissionId").value(firstSubmission))
                .andExpect(jsonPath("$[0].studentName").value("Ana Estudiante"))
                .andExpect(jsonPath("$[0].rubricVersion").value(1));
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(ordinaryAdmin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(student))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/submissions/" + firstSubmission + "/grade").cookie(student))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.score").doesNotExist());

        String grade = "/api/v1/submissions/" + firstSubmission + "/grade";
        mvc.perform(json(post(grade).cookie(ordinaryAdmin), "{\"score\":4.5}")).andExpect(status().isForbidden());
        // Etapa 9: un docente de otro curso no ve ni califica este (404, no revela que existe).
        mvc.perform(json(post(grade).cookie(otherTeacher), "{\"score\":4.5}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/submissions/" + firstSubmission).cookie(otherTeacher))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(otherTeacher))
                .andExpect(status().isNotFound());
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(otherTeacher),
                        activity("Intrusa", "No.", null, 1)))
                .andExpect(status().isNotFound());
        mvc.perform(json(post(grade).cookie(teacher), "{\"score\":5.5}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(json(post(grade).cookie(teacher), "{\"score\":4.5,\"feedback\":\"Bien planteado.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.grade.score").value(4.5))
                .andExpect(jsonPath("$.grade.gradedByName").value("Docente Curso"))
                .andExpect(jsonPath("$.history.length()").value(1));
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(teacher))
                .andExpect(jsonPath("$.length()").value(0));

        // RN-09: el docente corrige una nota publicada; queda el historial.
        mvc.perform(json(post(grade).cookie(teacher), "{\"score\":4,\"feedback\":\"Falta validar la entrada.\"}"))
                .andExpect(jsonPath("$.grade.score").value(4.0))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[1].previousScore").value(4.5));
        mvc.perform(get(grade).cookie(student))
                .andExpect(jsonPath("$.grade.score").value(4.0))
                .andExpect(jsonPath("$.grade.feedback").value("Falta validar la entrada."))
                .andExpect(jsonPath("$.history.length()").value(0))
                .andExpect(jsonPath("$.rubric.expectedAnswer").doesNotExist());
        mvc.perform(get("/api/v1/courses/" + courseId + "/grades/mine").cookie(student))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].score").value(4.0));
        mvc.perform(get("/api/v1/activities/" + activityId + "/grades").cookie(ordinaryAdmin))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].grade.score").value(4.0));
    }

    @Test
    @Order(4)
    void editingTheStatementCreatesANewRubricVersion() throws Exception {
        mvc.perform(json(put("/api/v1/activities/" + activityId).cookie(teacher),
                        activity("Algoritmo del promedio", "Escribe el pseudocódigo y pruébalo.", "60", 1)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/activities/" + activityId + "/rubric/versions").cookie(teacher))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value(2))
                .andExpect(jsonPath("$[0].statement").value("Escribe el pseudocódigo y pruébalo."))
                .andExpect(jsonPath("$[0].criteria.length()").value(2));
        // La entrega anterior conserva la versión con que se evaluó.
        mvc.perform(get("/api/v1/submissions/" + firstSubmission + "/grade").cookie(teacher))
                .andExpect(jsonPath("$.rubric.number").value(1))
                .andExpect(jsonPath("$.rubric.statement").value("Escribe el pseudocódigo."));
    }

    @Test
    @Order(5)
    void closingDateAndDeadlineExceptionsAreRespected() throws Exception {
        CLOCK.advance(Duration.ofDays(3));
        String url = "/api/v1/activities/" + activityId + "/submissions";

        // RN-05: vencida sin entrega cuenta como 0,0; ya no se puede entregar.
        mvc.perform(get("/api/v1/activities/" + activityId).cookie(classmate))
                .andExpect(jsonPath("$.progress.state").value("OVERDUE"))
                .andExpect(jsonPath("$.progress.canSubmit").value(false));
        mvc.perform(multipart(url).param("text", "tarde").cookie(classmate).with(csrf()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La actividad ya cerró. Solo el docente puede ampliar tu plazo."));

        String exceptions = "/api/v1/activities/" + activityId + "/exceptions/";
        mvc.perform(json(put(exceptions + classmateUserId).cookie(teacher),
                        "{\"extendedUntil\":\"" + CLOCK.instant().minus(Duration.ofHours(1)) + "\"}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(json(put(exceptions + userId("600002")).cookie(teacher),
                        "{\"extendedUntil\":\"" + CLOCK.instant().plus(Duration.ofDays(1)) + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(json(put(exceptions + classmateUserId).cookie(ordinaryAdmin),
                        "{\"extendedUntil\":\"" + CLOCK.instant().plus(Duration.ofDays(1)) + "\"}"))
                .andExpect(status().isForbidden());

        // Reapertura para quien no entregó.
        mvc.perform(json(put(exceptions + classmateUserId).cookie(teacher),
                        "{\"extendedUntil\":\"" + CLOCK.instant().plus(Duration.ofDays(1))
                                + "\",\"reason\":\"Incapacidad médica\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/activities/" + activityId).cookie(classmate))
                .andExpect(jsonPath("$.progress.state").value("OPEN"))
                .andExpect(jsonPath("$.progress.hasException").value(true));
        mvc.perform(multipart(url).param("text", "Mi solución").cookie(classmate).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.late").value(true));

        // Segundo intento: intentos adicionales con plazo ampliado.
        mvc.perform(json(put(exceptions + studentUserId).cookie(teacher),
                        "{\"extraAttempts\":1}"))
                .andExpect(status().isNoContent());
        mvc.perform(multipart(url).param("text", "Segundo intento").cookie(student).with(csrf()))
                .andExpect(status().isUnprocessableContent()); // sin plazo nuevo sigue cerrada
        mvc.perform(json(put(exceptions + studentUserId).cookie(teacher),
                        "{\"extendedUntil\":\"" + CLOCK.instant().plus(Duration.ofDays(1)) + "\",\"extraAttempts\":1}"))
                .andExpect(status().isNoContent());
        mvc.perform(multipart(url).param("text", "Segundo intento").cookie(student).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attempt").value(2))
                .andExpect(jsonPath("$.late").value(true));
        mvc.perform(multipart(url).param("text", "Tercero").cookie(student).with(csrf()))
                .andExpect(status().isUnprocessableContent());

        // El último intento vuelve a Por revisar, con la rúbrica vigente.
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(teacher))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.studentName == 'Ana Estudiante')].attempt").value(2))
                .andExpect(jsonPath("$[0].rubricVersion").value(2));
        mvc.perform(get("/api/v1/courses/" + courseId + "/grades/mine").cookie(student))
                .andExpect(jsonPath("$[0].attempt").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$[0].score").doesNotExist());

        mvc.perform(delete(exceptions + classmateUserId).cookie(teacher).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/activities/" + activityId).cookie(classmate))
                .andExpect(jsonPath("$.progress.hasException").value(false))
                .andExpect(jsonPath("$.progress.state").value("SUBMITTED"));
    }

    @Test
    @Order(6)
    void archivedActivityLeavesTheCourseAndFreesItsWeight() throws Exception {
        mvc.perform(delete("/api/v1/activities/" + activityId).cookie(teacher).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/courses/" + courseId + "/activities").cookie(student))
                .andExpect(jsonPath("$.length()").value(0));
        closesAt = null;
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Nueva", "Otra actividad.", "100", 2)))
                .andExpect(status().isCreated());
    }

    // ---------- utilidades ----------

    private String activity(String title, String statement, String weight, int attempts) {
        Instant opens = CLOCK.instant().minus(Duration.ofHours(1));
        Instant closes = closesAt != null ? closesAt : CLOCK.instant().plus(Duration.ofDays(2));
        return "{\"title\":\"" + title + "\",\"kind\":\"TASK\",\"statement\":\"" + statement + "\","
                + "\"acceptsText\":true,\"acceptsFiles\":true,\"acceptsPhotos\":false,\"fileGroups\":[\"DOCUMENTS\"],"
                + "\"weightPercent\":" + weight + ",\"opensAt\":\"" + opens + "\",\"closesAt\":\"" + closes + "\","
                + "\"maxAttempts\":" + attempts + "}";
    }

    private static byte[] pdf() {
        return "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n".getBytes();
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String userId(String cedula) throws Exception {
        String body = mvc.perform(get("/api/v1/admin/users").param("size", "100").cookie(admin))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.items[?(@.cedula == '" + cedula + "')].id");
        assertThat(ids).hasSize(1);
        return ids.getFirst();
    }

    private void register(String path, String cedula, String name, String email) throws Exception {
        mvc.perform(json(post("/api/v1/auth/" + path), "{\"cedula\":\"" + cedula + "\",\"fullName\":\"" + name
                        + "\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
    }

    private Cookie login(String cedula) throws Exception {
        MvcResult result = mvc.perform(json(post("/api/v1/auth/login"),
                        "{\"cedula\":\"" + cedula + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("LOGICODE_SESSION");
    }

    private void recover(String email) throws Exception {
        mvc.perform(json(post("/api/v1/auth/password-recovery"), "{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
    }

    private void reset(String token, String password) throws Exception {
        mvc.perform(json(post("/api/v1/auth/password-reset"),
                        "{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    private void confirmEmail(String token) throws Exception {
        mvc.perform(json(post("/api/v1/auth/email-verifications/confirm"), "{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String lastTokenFor(String email) {
        EmailMessage message = OUTBOX.reversed().stream().filter(m -> m.toAddress().equals(email)).findFirst()
                .orElseThrow(() -> new AssertionError("No hay correo para " + email));
        Matcher matcher = TOKEN.matcher(message.textBody());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
