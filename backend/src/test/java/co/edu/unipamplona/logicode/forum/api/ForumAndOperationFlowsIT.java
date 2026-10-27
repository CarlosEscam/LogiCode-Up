package co.edu.unipamplona.logicode.forum.api;

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
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.MediaType;
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
 * Pruebas de aceptación de la Etapa 8 contra PostgreSQL real: foro con moderación, planilla con pesos (RN-07),
 * exportación conciliada con la pantalla, panel de administración y respaldos. Punto de control: permisos
 * administrativos revisados y exportación conciliada con la escala y los pesos.
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
class ForumAndOperationFlowsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    static final List<EmailMessage> OUTBOX = new CopyOnWriteArrayList<>();
    static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    static final String PASSWORD = "Clave#2026";
    static final MovableClock CLOCK = new MovableClock(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    static Path backupsDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        Path files = Files.createTempDirectory("logicode-stage8-files");
        backupsDir = Files.createTempDirectory("logicode-stage8-backups");
        registry.add("logicode.files.storage-dir", files::toString);
        registry.add("logicode.backups.dir", backupsDir::toString);
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
    String courseId;
    String threadId;
    String replyId;
    String workshop1;
    String workshop2;
    String workshop3;

    @BeforeAll
    void createPeopleAndCourse() throws Exception {
        confirmEmail(lastTokenFor("admin1@example.com"));
        mvc.perform(json(post("/api/v1/auth/password-recovery"), "{\"email\":\"admin1@example.com\"}"))
                .andExpect(status().isAccepted());
        mvc.perform(json(post("/api/v1/auth/password-reset"),
                        "{\"token\":\"" + lastTokenFor("admin1@example.com") + "\",\"newPassword\":\"" + PASSWORD
                                + "\"}"))
                .andExpect(status().isOk());
        admin = login("900001");

        register("teacher-requests", "500001", "Docente Curso", "docente@example.com");
        mvc.perform(post("/api/v1/admin/teachers/" + userId("500001") + "/approve").cookie(admin).with(csrf()))
                .andExpect(status().isOk());
        teacher = login("500001");

        courseId = JsonPath.read(mvc.perform(json(post("/api/v1/courses").cookie(teacher),
                        "{\"name\":\"Lógica de programación\",\"period\":\"2026-2\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        for (String cedula : List.of("600001", "600004", "600009")) {
            mvc.perform(json(post("/api/v1/courses/" + courseId + "/enrollments").cookie(teacher),
                            "{\"cedula\":\"" + cedula + "\"" + (cedula.equals("600009")
                                    ? ",\"fullName\":\"Zoe Sin Registro\"" : "") + "}"))
                    .andExpect(status().isCreated());
        }
        register("student-registrations", "600001", "Ana Estudiante", "ana@example.com");
        register("student-registrations", "600004", "Beto Estudiante", "beto@example.com");
        student = login("600001");
        classmate = login("600004");

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

    // ---------- Foro (RF-26, RF-27, RN-15) ----------

    @Test
    @Order(1)
    void visitorsReadTheForumButOnlyAccountsWrite() throws Exception {
        mvc.perform(get("/api/v1/forum/threads")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(json(post("/api/v1/forum/threads"),
                        "{\"category\":\"PSEINT\",\"title\":\"Hola\",\"body\":\"Sin sesión\"}"))
                .andExpect(status().isUnauthorized());

        threadId = id(mvc.perform(json(post("/api/v1/forum/threads").cookie(student),
                        "{\"category\":\"PSEINT\",\"title\":\"¿Cómo uso Mientras?\",\"body\":\"No sé cuándo para.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Ana Estudiante"))
                .andExpect(jsonPath("$.canModerate").value(false)));
        mvc.perform(json(post("/api/v1/forum/threads").cookie(teacher),
                        "{\"category\":\"GENERAL\",\"title\":\"Normas del foro\",\"body\":\"Respeto ante todo.\"}"))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/v1/forum/threads").cookie(student),
                        "{\"category\":\"KOTLIN\",\"title\":\"Otra\",\"body\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/v1/forum/threads").cookie(student),
                        "{\"category\":\"DFD\",\"title\":\"   \",\"body\":\"x\"}"))
                .andExpect(status().isBadRequest());

        replyId = JsonPath.read(mvc.perform(json(post("/api/v1/forum/threads/" + threadId + "/posts").cookie(classmate),
                        "{\"body\":\"Para cuando la condición es falsa.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andReturn().getResponse().getContentAsString(), "$.posts[0].id");

        mvc.perform(get("/api/v1/forum/threads/" + threadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].authorName").value("Beto Estudiante"))
                .andExpect(jsonPath("$.canReply").value(false))
                .andExpect(jsonPath("$.posts[0].cedula").doesNotExist());
        mvc.perform(get("/api/v1/forum/threads").param("category", "PSEINT"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].replyCount").value(1));
        mvc.perform(get("/api/v1/forum/threads").param("q", "respeto"))
                .andExpect(jsonPath("$.items[0].title").value("Normas del foro"));
    }

    @Test
    @Order(2)
    void teacherAndAdminsModerateStudentsDoNot() throws Exception {
        String url = "/api/v1/forum/threads/" + threadId;
        mvc.perform(json(patch(url).cookie(student), "{\"pinned\":true}")).andExpect(status().isForbidden());
        mvc.perform(json(patch(url).cookie(teacher), "{\"pinned\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.canModerate").value(true));
        // Fijado: queda primero aunque el otro hilo sea más reciente.
        mvc.perform(get("/api/v1/forum/threads")).andExpect(jsonPath("$.items[0].id").value(threadId));

        mvc.perform(json(patch(url).cookie(ordinaryAdmin), "{\"closed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$.pinned").value(true));
        mvc.perform(json(post(url + "/posts").cookie(classmate), "{\"body\":\"Otra respuesta\"}"))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/v1/forum/posts/" + replyId).cookie(student).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/forum/posts/" + replyId).cookie(ordinaryAdmin).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(jsonPath("$.posts.length()").value(0));
        mvc.perform(get("/api/v1/forum/threads").param("category", "PSEINT"))
                .andExpect(jsonPath("$.items[0].replyCount").value(0));

        mvc.perform(delete(url).cookie(teacher).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/forum/threads")).andExpect(jsonPath("$.totalItems").value(1));
    }

    // ---------- Planilla, estadísticas y exportación (RF-18, RF-20, RN-07) ----------

    @Test
    @Order(3)
    void gradebookWeighsActivitiesAndKeepsPartialApartFromDefinitive() throws Exception {
        Instant now = CLOCK.instant();
        workshop1 = publish("Taller 1", "50", now.plus(Duration.ofDays(1)));
        workshop2 = publish("Taller 2", null, now.plus(Duration.ofDays(2)));
        workshop3 = publish("Taller 3", null, now.plus(Duration.ofDays(3)));
        // Un borrador no entra en la planilla.
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Borrador", "30", now.plus(Duration.ofDays(5)))))
                .andExpect(status().isCreated());

        grade(submit(workshop1, student), "4.0");
        grade(submit(workshop2, student), "2.0");
        submit(workshop1, classmate);

        String book = "/api/v1/courses/" + courseId + "/gradebook";
        mvc.perform(get(book).cookie(teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullView").value(true))
                .andExpect(jsonPath("$.columns.length()").value(3))
                .andExpect(jsonPath("$.columns[0].title").value("Taller 1"))
                .andExpect(jsonPath("$.columns[1].effectiveWeight").value(25.0))
                .andExpect(jsonPath("$.weightsComplete").value(true))
                .andExpect(jsonPath("$.rows.length()").value(3))
                // Ana: (50 × 4,0 + 25 × 2,0) / 75 = 3,3; sin definitiva mientras falte el Taller 3.
                .andExpect(jsonPath("$.rows[0].fullName").value("Ana Estudiante"))
                .andExpect(jsonPath("$.rows[0].partial").value(3.3))
                .andExpect(jsonPath("$.rows[0].definitive").doesNotExist())
                .andExpect(jsonPath("$.rows[1].cells[0].status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.rows[1].partial").doesNotExist())
                .andExpect(jsonPath("$.rows[2].fullName").value("Zoe Sin Registro"))
                .andExpect(jsonPath("$.rows[2].registered").value(false))
                .andExpect(jsonPath("$.columns[0].stats.submitted").value(2))
                .andExpect(jsonPath("$.columns[0].stats.graded").value(1))
                .andExpect(jsonPath("$.columns[0].stats.inReview").value(1))
                .andExpect(jsonPath("$.columns[0].stats.average").value(4.0));

        // RF-20: el estudiante ve solo su fila y sin estadísticas del grupo.
        mvc.perform(get(book).cookie(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullView").value(false))
                .andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(jsonPath("$.rows[0].cedula").value("600001"))
                .andExpect(jsonPath("$.columns[0].stats").doesNotExist())
                .andExpect(jsonPath("$.stats").doesNotExist());
        mvc.perform(get(book).cookie(outsider)).andExpect(status().isNotFound());
        mvc.perform(get(book).cookie(ordinaryAdmin)).andExpect(jsonPath("$.fullView").value(true));
    }

    @Test
    @Order(4)
    void afterClosingOverdueCountsZeroAndExportMatchesTheScreen() throws Exception {
        CLOCK.advance(Duration.ofDays(4));
        String book = "/api/v1/courses/" + courseId + "/gradebook";
        String json = mvc.perform(get(book).cookie(teacher))
                // Ana: Taller 3 vencido = 0,0 → (200 + 50 + 0) / 100 = 2,5.
                .andExpect(jsonPath("$.rows[0].cells[2].status").value("OVERDUE"))
                .andExpect(jsonPath("$.rows[0].definitive").value(2.5))
                .andExpect(jsonPath("$.rows[0].partial").value(2.5))
                // Beto: el Taller 1 sigue en revisión, así que no tiene definitiva.
                .andExpect(jsonPath("$.rows[1].partial").value(0.0))
                .andExpect(jsonPath("$.rows[1].definitive").doesNotExist())
                .andExpect(jsonPath("$.rows[2].definitive").value(0.0))
                .andExpect(jsonPath("$.stats.withDefinitive").value(2))
                .andExpect(jsonPath("$.stats.passingDefinitive").value(0))
                .andReturn().getResponse().getContentAsString();

        MvcResult export = mvc.perform(get(book + "/export").cookie(teacher))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andReturn();
        String csv = new String(export.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿Planilla;Lógica de programación;2026-2");
        assertThat(csv).contains("Cédula;Estudiante;Taller 1;Taller 2;Taller 3;Avance parcial;Nota definitiva");
        assertThat(csv).contains(";Porcentaje;50 %;25 %;25 %\r\n");
        // Conciliación: las cifras de la exportación son las de la pantalla.
        Number anaDefinitive = JsonPath.read(json, "$.rows[0].definitive");
        assertThat(csv).contains("600001;Ana Estudiante;4,0;2,0;0,0;2,5;" + anaDefinitive.toString().replace('.', ','));
        assertThat(csv).contains("600004;Beto Estudiante;En revisión;0,0;0,0;0,0;\r\n");
        assertThat(csv).contains("600009;Zoe Sin Registro;0,0;0,0;0,0;0,0;0,0");

        mvc.perform(get(book + "/export").cookie(student)).andExpect(status().isForbidden());
        mvc.perform(get(book + "/export").cookie(ordinaryAdmin)).andExpect(status().isOk());
    }

    // ---------- Panel de administración y respaldos (RNF-09, RF-28) ----------

    @Test
    @Order(5)
    void adminPanelShowsPlatformFiguresOnlyToAdmins() throws Exception {
        mvc.perform(get("/api/v1/admin/overview").cookie(ordinaryAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTeachers").value(1))
                .andExpect(jsonPath("$.activeAdmins").value(2))
                .andExpect(jsonPath("$.courses").value(1))
                .andExpect(jsonPath("$.publishedActivities").value(3))
                .andExpect(jsonPath("$.submissions").value(3))
                .andExpect(jsonPath("$.gradesInReview").value(1))
                .andExpect(jsonPath("$.forumThreads").value(1));
        mvc.perform(get("/api/v1/admin/overview").cookie(teacher)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/overview").cookie(student)).andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    void adminsListAndRequestBackups() throws Exception {
        mvc.perform(get("/api/v1/admin/backups").cookie(ordinaryAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service.running").value(false))
                .andExpect(jsonPath("$.items.length()").value(0));

        Path done = Files.createDirectories(backupsDir.resolve("logicode-20261027-020000"));
        Files.write(done.resolve("base.dump"), new byte[2048]);
        Files.write(done.resolve("archivos.tar.gz"), new byte[512]);
        Files.writeString(done.resolve("prueba-restauracion.txt"), "OK\nBase restaurada en una copia temporal.\n");
        Files.createDirectories(backupsDir.resolve(".logicode-20261027-030000.parcial"));
        Files.writeString(backupsDir.resolve("estado.txt"), "hora=2\nretencion_dias=30\n");

        mvc.perform(get("/api/v1/admin/backups").cookie(admin))
                .andExpect(jsonPath("$.service.running").value(true))
                .andExpect(jsonPath("$.service.dailyHour").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].databaseBytes").value(2048))
                .andExpect(jsonPath("$.items[0].restoreTest").value("OK"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-10-27T07:00:00Z"));

        mvc.perform(post("/api/v1/admin/backups").cookie(teacher).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/backups").cookie(ordinaryAdmin).with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pendingRequest").value(true));
        mvc.perform(post("/api/v1/admin/backups").cookie(ordinaryAdmin).with(csrf()))
                .andExpect(status().isConflict());
        try (var requests = Files.list(backupsDir.resolve("solicitudes"))) {
            assertThat(requests.count()).isEqualTo(1);
        }
    }

    // ---------- utilidades ----------

    private String activity(String title, String weight, Instant closes) {
        Instant opens = CLOCK.instant().minus(Duration.ofHours(1));
        return "{\"title\":\"" + title + "\",\"kind\":\"WORKSHOP\",\"statement\":\"Resuelve el taller.\","
                + "\"acceptsText\":true,\"acceptsFiles\":false,\"acceptsPhotos\":false,\"fileGroups\":[],"
                + "\"weightPercent\":" + weight + ",\"opensAt\":\"" + opens + "\",\"closesAt\":\"" + closes + "\","
                + "\"maxAttempts\":1}";
    }

    private String publish(String title, String weight, Instant closes) throws Exception {
        String id = id(mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                activity(title, weight, closes))).andExpect(status().isCreated()));
        mvc.perform(json(put("/api/v1/activities/" + id + "/rubric").cookie(teacher),
                        "{\"criteria\":[{\"name\":\"Solución\",\"maxPoints\":5}]}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/activities/" + id + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isOk());
        return id;
    }

    private String submit(String activityId, Cookie who) throws Exception {
        return id(mvc.perform(multipart("/api/v1/activities/" + activityId + "/submissions")
                .param("text", "Mi respuesta").cookie(who).with(csrf())).andExpect(status().isCreated()));
    }

    private void grade(String submissionId, String score) throws Exception {
        mvc.perform(json(post("/api/v1/submissions/" + submissionId + "/grade").cookie(teacher),
                        "{\"score\":" + score + "}"))
                .andExpect(status().isOk());
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
