package co.edu.unipamplona.logicode.grading.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import co.edu.unipamplona.logicode.grading.application.GradingProvider;
import co.edu.unipamplona.logicode.grading.application.RunAiGrading;
import co.edu.unipamplona.logicode.identity.application.EmailMessage;
import co.edu.unipamplona.logicode.identity.application.EmailSender;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas de la Etapa 6 contra PostgreSQL real con un proveedor de IA simulado: publicación automática desde el 80 %
 * de confianza, confirmación docente para ilegibles y baja confianza, fallos técnicos sin nota y con reintentos, y
 * ningún resultado de la IA pisa una nota puesta por el docente. Estas pruebas no miden el modelo real: eso lo hacen
 * las pruebas de aceptación de {@code ia-pruebas/} en el equipo de presentación.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "logicode.identity.initial-admins[0].cedula=900001",
        "logicode.identity.initial-admins[0].full-name=Administradora Uno",
        "logicode.identity.initial-admins[0].email=admin1@example.com",
        "logicode.ai.enabled=true",
        "logicode.ai.worker-enabled=false",
        "logicode.ai.max-attempts=3",
        "logicode.ai.retry-delay=2m"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AiGradingFlowsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    static final List<EmailMessage> OUTBOX = new CopyOnWriteArrayList<>();
    static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    static final String PASSWORD = "Clave#2026";
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    static final MovableClock CLOCK = new MovableClock(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    static final ScriptedProvider PROVIDER = new ScriptedProvider();

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        Path dir = Files.createTempDirectory("logicode-ai-it");
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

        @Bean
        @Primary
        GradingProvider scriptedProvider() {
            return PROVIDER;
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

    /** Proveedor que responde lo que cada prueba le indica y guarda lo que recibió. */
    static final class ScriptedProvider implements GradingProvider {

        final Deque<Supplier<Result>> answers = new ArrayDeque<>();
        final List<Request> received = new CopyOnWriteArrayList<>();

        void answer(boolean legible, int confidence, String... points) {
            answers.add(() -> new Result("Leer n1, n2; prom <- (n1+n2)/2", legible,
                    List.of(points).stream().map(p -> new CriterionResult(new BigDecimal(p), "Comentario")).toList(),
                    "Buen trabajo, revisa la salida.", confidence));
        }

        void fail(String message) {
            answers.add(() -> {
                throw new GradingFailure(message);
            });
        }

        @Override
        public String model() {
            return "simulado";
        }

        @Override
        public Result evaluate(Request request) {
            received.add(request);
            Supplier<Result> next = answers.poll();
            if (next == null) {
                throw new AssertionError("El proveedor simulado no tenía respuesta preparada");
            }
            return next.get();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    RunAiGrading worker;

    Cookie admin;
    Cookie teacher;
    Cookie ana;
    Cookie beto;
    String courseId;
    String activityId;

    @BeforeAll
    void createPeopleCourseAndActivity() throws Exception {
        confirmEmail(lastTokenFor("admin1@example.com"));
        recover("admin1@example.com");
        reset(lastTokenFor("admin1@example.com"));
        admin = login("900001");

        register("teacher-requests", "500001", "Docente Curso", "docente@example.com");
        mvc.perform(post("/api/v1/admin/teachers/" + userId("500001") + "/approve").cookie(admin).with(csrf()))
                .andExpect(status().isOk());
        teacher = login("500001");

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
        ana = login("600001");
        beto = login("600004");

        Instant opens = CLOCK.instant().minus(Duration.ofHours(1));
        Instant closes = CLOCK.instant().plus(Duration.ofDays(5));
        activityId = id(mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        "{\"title\":\"Promedio a mano\",\"kind\":\"TASK\",\"statement\":\"Escribe el pseudocódigo.\","
                                + "\"acceptsText\":true,\"acceptsFiles\":true,\"acceptsPhotos\":true,"
                                + "\"fileGroups\":[\"DOCUMENTS\",\"TOOL_FILES\"],\"weightPercent\":20,"
                                + "\"opensAt\":\"" + opens + "\",\"closesAt\":\"" + closes + "\",\"maxAttempts\":9}"))
                .andExpect(status().isCreated()));
        mvc.perform(json(put("/api/v1/activities/" + activityId + "/rubric").cookie(teacher),
                        "{\"expectedAnswer\":\"prom <- (n1+n2)/2\",\"criteria\":["
                                + "{\"name\":\"Lectura de datos\",\"maxPoints\":6},"
                                + "{\"name\":\"Cálculo\",\"maxPoints\":4}]}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/activities/" + activityId + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(1)
    void legibleAndConfidentResultIsPublishedAutomatically() throws Exception {
        String submission = submit(ana, photo().param("text", "mi solución"));
        mvc.perform(get(grade(submission)).cookie(ana))
                .andExpect(jsonPath("$.grade.status").value("PROCESSING"))
                .andExpect(jsonPath("$.grade.score").doesNotExist());
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(teacher))
                .andExpect(jsonPath("$[?(@.submissionId == '" + submission + "')].status").value("PROCESSING"));

        PROVIDER.answer(true, 92, "6", "3");
        assertThat(worker.processNext()).isTrue();
        assertThat(worker.processNext()).isFalse();

        GradingProvider.Request request = PROVIDER.received.getLast();
        assertThat(request.studentName()).isEqualTo("Ana Estudiante");
        assertThat(request.images()).hasSize(1);
        assertThat(request.images().getFirst().label()).isEqualTo("Imagen 1");
        assertThat(request.toString()).doesNotContain("600001").doesNotContain("ana@example.com")
                .doesNotContain("cuaderno.png");
        assertThat(request.expectedAnswer()).isEqualTo("prom <- (n1+n2)/2");

        mvc.perform(get(grade(submission)).cookie(ana))
                .andExpect(jsonPath("$.grade.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.grade.score").value(4.5))
                .andExpect(jsonPath("$.grade.source").value("AI"))
                .andExpect(jsonPath("$.grade.feedback").value("Buen trabajo, revisa la salida."))
                .andExpect(jsonPath("$.grade.reviewReason").doesNotExist())
                .andExpect(jsonPath("$.aiEvaluations.length()").value(0));
        mvc.perform(get(grade(submission)).cookie(teacher))
                .andExpect(jsonPath("$.aiEvaluations[0].outcome").value("PUBLISHED"))
                .andExpect(jsonPath("$.aiEvaluations[0].confidence").value(92))
                .andExpect(jsonPath("$.aiEvaluations[0].legible").value(true))
                .andExpect(jsonPath("$.aiEvaluations[0].proposedScore").value(4.5))
                .andExpect(jsonPath("$.aiEvaluations[0].criteria[1].name").value("Cálculo"))
                .andExpect(jsonPath("$.aiEvaluations[0].criteria[1].points").value(3))
                .andExpect(jsonPath("$.history[0].source").value("AI"))
                .andExpect(jsonPath("$.canRetryWithAi").value(false));
        mvc.perform(post(grade(submission) + "/ai-retry").cookie(teacher).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(2)
    void lowConfidenceWaitsForTheTeacherWhoConfirmsTheProposal() throws Exception {
        String submission = submit(beto, multipart(submissions()).param("text", "prom <- n1 + n2 / 2"));
        PROVIDER.answer(true, 79, "6", "1");
        assertThat(worker.processNext()).isTrue();

        mvc.perform(get(grade(submission)).cookie(teacher))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.reviewReason").value("LOW_CONFIDENCE"))
                .andExpect(jsonPath("$.grade.score").doesNotExist())
                .andExpect(jsonPath("$.aiEvaluations[0].proposedScore").value(3.5))
                .andExpect(jsonPath("$.canRetryWithAi").value(true));
        mvc.perform(get(grade(submission)).cookie(beto))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.reviewReason").doesNotExist());
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(teacher))
                .andExpect(jsonPath("$[?(@.submissionId == '" + submission + "')].reviewReason")
                        .value("LOW_CONFIDENCE"));

        mvc.perform(json(post(grade(submission)).cookie(teacher), "{\"score\":3.5,\"feedback\":\"Confirmada.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.grade.source").value("TEACHER"));
    }

    @Test
    @Order(3)
    void illegiblePhotoAsksForTeacherConfirmationWithoutScore() throws Exception {
        String submission = submit(ana, photo());
        PROVIDER.answer(false, 30);
        assertThat(worker.processNext()).isTrue();
        mvc.perform(get(grade(submission)).cookie(teacher))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.reviewReason").value("ILLEGIBLE"))
                .andExpect(jsonPath("$.aiEvaluations[0].outcome").value("ILLEGIBLE"))
                .andExpect(jsonPath("$.aiEvaluations[0].proposedScore").doesNotExist());
    }

    @Test
    @Order(4)
    void technicalFailuresAreRetriedThenGoToTheTeacherWithoutScore() throws Exception {
        String submission = submit(ana, photo());
        PROVIDER.fail("No fue posible conectar con el servicio de IA (Ollama) o no respondió a tiempo.");
        assertThat(worker.processNext()).isTrue();
        assertThat(worker.processNext()).as("espera antes de reintentar").isFalse();
        mvc.perform(get(grade(submission)).cookie(teacher))
                .andExpect(jsonPath("$.grade.status").value("PROCESSING"))
                .andExpect(jsonPath("$.aiEvaluations[0].outcome").value("FAILED"));

        CLOCK.advance(Duration.ofMinutes(3));
        PROVIDER.answer(true, 95, "9", "4"); // 9 de 6: respuesta incoherente con la rúbrica
        assertThat(worker.processNext()).isTrue();
        CLOCK.advance(Duration.ofMinutes(5));
        PROVIDER.fail("El servicio de IA respondió 500: sin memoria");
        assertThat(worker.processNext()).isTrue();

        mvc.perform(get(grade(submission)).cookie(teacher))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.reviewReason").value("AI_FAILED"))
                .andExpect(jsonPath("$.grade.score").doesNotExist())
                .andExpect(jsonPath("$.history.length()").value(0))
                .andExpect(jsonPath("$.aiEvaluations.length()").value(3))
                .andExpect(jsonPath("$.aiEvaluations[1].error").value(
                        "Puntos fuera de rango en el criterio \"Lectura de datos\": 9 de 6.00"))
                .andExpect(jsonPath("$.canRetryWithAi").value(true));

        mvc.perform(post(grade(submission) + "/ai-retry").cookie(ana).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(grade(submission) + "/ai-retry").cookie(teacher).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade.status").value("PROCESSING"));
        mvc.perform(post(grade(submission) + "/ai-retry").cookie(teacher).with(csrf()))
                .andExpect(status().isConflict());
        PROVIDER.answer(true, 85, "5", "4");
        assertThat(worker.processNext()).isTrue();
        mvc.perform(get(grade(submission)).cookie(ana))
                .andExpect(jsonPath("$.grade.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.grade.score").value(4.5));
    }

    @Test
    @Order(5)
    void aiResultNeverOverwritesTheTeachersGrade() throws Exception {
        String submission = submit(beto, multipart(submissions()).param("text", "segundo intento"));
        mvc.perform(json(post(grade(submission)).cookie(teacher), "{\"score\":2.0}"))
                .andExpect(status().isOk());
        assertThat(worker.processNext()).as("la nota ya no está en evaluación: no llama al modelo").isTrue();
        mvc.perform(get(grade(submission)).cookie(teacher))
                .andExpect(jsonPath("$.grade.score").value(2.0))
                .andExpect(jsonPath("$.grade.source").value("TEACHER"))
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.aiEvaluations.length()").value(0));
        assertThat(PROVIDER.answers).isEmpty();
    }

    @Test
    @Order(6)
    void formatsTheModelCannotReadGoStraightToTheTeacher() throws Exception {
        String submission = submit(ana, multipart(submissions()).file(new MockMultipartFile("files", "solucion.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx())));
        assertThat(worker.processNext()).isFalse();
        mvc.perform(get(grade(submission)).cookie(teacher))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.reviewReason").value("UNSUPPORTED"))
                .andExpect(jsonPath("$.canRetryWithAi").value(false));
        mvc.perform(post(grade(submission) + "/ai-retry").cookie(teacher).with(csrf()))
                .andExpect(status().isUnprocessableContent());

        // P-08: un PDF dañado se intenta leer y, al no poder, pasa al docente sin llamar al modelo.
        String broken = submit(ana, multipart(submissions()).file(new MockMultipartFile("files", "solucion.pdf",
                "application/pdf", "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n".getBytes())));
        int calls = PROVIDER.received.size();
        assertThat(worker.processNext()).isTrue();
        assertThat(PROVIDER.received).hasSize(calls);
        mvc.perform(get(grade(broken)).cookie(teacher))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.reviewReason").value("UNSUPPORTED"));

        // P-08: un PDF llega al modelo con su texto y con cada página como imagen.
        String pdf = submit(ana, multipart(submissions()).file(new MockMultipartFile("files", "solucion.pdf",
                "application/pdf", pdf("Leer n1, n2", "prom <- (n1 + n2) / 2"))));
        PROVIDER.answer(true, 90, "6", "4");
        assertThat(worker.processNext()).isTrue();
        GradingProvider.Request fromPdf = PROVIDER.received.getLast();
        assertThat(fromPdf.textFiles()).singleElement().satisfies(file -> {
            assertThat(file.label()).isEqualTo("Archivo 1");
            assertThat(file.format()).isEqualTo("PDF, texto extraído");
            assertThat(file.content()).contains("Leer n1, n2").contains("prom <- (n1 + n2) / 2");
        });
        assertThat(fromPdf.images()).hasSize(1);
        mvc.perform(get(grade(pdf)).cookie(ana)).andExpect(jsonPath("$.grade.score").value(5.0));

        String pseint = submit(ana, multipart(submissions()).file(new MockMultipartFile("files", "promedio.psc",
                "text/plain", "Algoritmo promedio\n  Leer n1, n2\nFinAlgoritmo\n".getBytes())));
        PROVIDER.answer(true, 88, "6", "4");
        assertThat(worker.processNext()).isTrue();
        GradingProvider.Request request = PROVIDER.received.getLast();
        assertThat(request.textFiles()).singleElement().satisfies(file -> {
            assertThat(file.label()).isEqualTo("Archivo 1");
            assertThat(file.format()).isEqualTo("PSC");
            assertThat(file.content()).contains("Leer n1, n2");
        });
        mvc.perform(get(grade(pseint)).cookie(ana)).andExpect(jsonPath("$.grade.score").value(5.0));
    }

    /** PDF de una página con el texto dado, como lo exporta un procesador de texto. */
    static byte[] pdf(String... lines) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream text = new PDPageContentStream(document, page)) {
                text.beginText();
                text.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                text.newLineAtOffset(72, 700);
                for (String line : lines) {
                    text.showText(line);
                    text.newLineAtOffset(0, -20);
                }
                text.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] docx() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes());
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document/>".getBytes());
        }
        return out.toByteArray();
    }

    private MockMultipartHttpServletRequestBuilder photo() {
        return multipart(submissions()).file(new MockMultipartFile("files", "cuaderno.png", "image/png", PNG));
    }

    private String submissions() {
        return "/api/v1/activities/" + activityId + "/submissions";
    }

    private static String grade(String submissionId) {
        return "/api/v1/submissions/" + submissionId + "/grade";
    }

    private String submit(Cookie student, MockMultipartHttpServletRequestBuilder request) throws Exception {
        return id(mvc.perform(request.cookie(student).with(csrf())).andExpect(status().isCreated()));
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

    private void reset(String token) throws Exception {
        mvc.perform(json(post("/api/v1/auth/password-reset"),
                        "{\"token\":\"" + token + "\",\"newPassword\":\"" + PASSWORD + "\"}"))
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
