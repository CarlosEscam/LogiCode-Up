package co.edu.unipamplona.logicode.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.edu.unipamplona.logicode.files.application.ExpiredFileCleanup;
import co.edu.unipamplona.logicode.identity.application.EmailMessage;
import co.edu.unipamplona.logicode.identity.application.EmailSender;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas de aceptación de la Etapa 4 contra PostgreSQL real. Punto de control: el visitante y quien participa en
 * el curso ven exactamente los recursos permitidos; la carga valida el contenido real de los archivos.
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
class ContentFlowsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    static final List<EmailMessage> OUTBOX = new CopyOnWriteArrayList<>();
    static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    static final String PASSWORD = "Clave#2026";
    static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    static Path storageDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        storageDir = Files.createTempDirectory("logicode-files-it");
        registry.add("logicode.files.storage-dir", storageDir::toString);
    }

    @TestConfiguration
    static class CapturingMail {
        @Bean
        @Primary
        EmailSender capturingEmailSender() {
            return OUTBOX::add;
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ExpiredFileCleanup cleanup;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    Cookie admin;
    Cookie teacher;
    Cookie member;
    Cookie outsider;
    Cookie ordinaryAdmin;
    String courseId;
    String topicId;
    String publicPdf;
    String privatePng;
    String privateLink;

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

        // Habilitada antes del primer curso: queda inscrita al crearlo.
        mvc.perform(json(post("/api/v1/student-enablements").cookie(teacher), "{\"cedula\":\"600001\"}"))
                .andExpect(status().isCreated());
        MvcResult course = mvc.perform(json(post("/api/v1/courses").cookie(teacher),
                        "{\"name\":\"Lógica de programación\",\"period\":\"2026-2\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Docente Curso"))
                .andExpect(jsonPath("$.enrolledCount").value(1))
                .andExpect(jsonPath("$.canManageContent").value(true))
                .andReturn();
        courseId = JsonPath.read(course.getResponse().getContentAsString(), "$.id");

        register("student-registrations", "600001", "Estudiante Inscrita", "inscrita@example.com");
        member = login("600001");

        // Inscripción por cédula desde el curso: también habilita la cédula.
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/enrollments").cookie(teacher),
                        "{\"cedula\":\"600003\",\"fullName\":\"Estudiante Tres\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registered").value(false));
        register("student-registrations", "600003", "Estudiante Tres", "tres@example.com");

        // Habilitada después: puede registrarse, pero no participa en el curso.
        mvc.perform(json(post("/api/v1/student-enablements").cookie(teacher), "{\"cedula\":\"600002\"}"))
                .andExpect(status().isCreated());
        register("student-registrations", "600002", "Estudiante Externa", "externa@example.com");
        outsider = login("600002");

        mvc.perform(json(patch("/api/v1/admin/users/" + userId("600003") + "/role").cookie(admin),
                        "{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk());
        ordinaryAdmin = login("600003");

        MvcResult topic = mvc.perform(json(post("/api/v1/courses/" + courseId + "/topics").cookie(teacher),
                        "{\"title\":\"Algoritmos con PSeInt\",\"tool\":\"PSEINT\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        topicId = JsonPath.read(topic.getResponse().getContentAsString(), "$.id");

        publicPdf = id(upload(teacher, "Guía de variables", "GUIDE", "PUBLIC",
                new MockMultipartFile("file", "guía variables.pdf", "application/pdf", PDF))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.file.format").value("PDF"))
                .andExpect(jsonPath("$.file.previewable").value(true)));
        privatePng = id(upload(teacher, "Diagrama de clase", "IMAGE", "PRIVATE",
                new MockMultipartFile("file", "diagrama.png", "image/png", PNG))
                .andExpect(status().isCreated()));
        privateLink = id(mvc.perform(multipart("/api/v1/topics/" + topicId + "/materials").cookie(teacher).with(csrf())
                        .param("title", "Descarga de PSeInt").param("kind", "LINK").param("visibility", "PRIVATE")
                        .param("url", "https://pseint.sourceforge.net/"))
                .andExpect(status().isCreated()));
    }

    @Test
    @Order(1)
    void visitorSeesOnlyPublicMaterials() throws Exception {
        mvc.perform(get("/api/v1/library"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(publicPdf))
                .andExpect(jsonPath("$.items[0].courseName").value("Lógica de programación"))
                .andExpect(jsonPath("$.items[0].canEdit").value(false));
        mvc.perform(get("/api/v1/library/topics")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/materials/" + privatePng)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/materials/" + privatePng + "/download")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/courses/" + courseId + "/topics")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/materials/" + publicPdf + "/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("inline")));
        mvc.perform(get("/api/v1/materials/" + publicPdf + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''gu%C3%ADa%20variables.pdf")));
        MvcResult range = mvc.perform(get("/api/v1/materials/" + publicPdf + "/preview")
                        .header(HttpHeaders.RANGE, "bytes=0-3"))
                .andExpect(status().isPartialContent())
                .andReturn();
        assertThat(range.getResponse().getContentAsString()).isEqualTo("%PDF");
    }

    @Test
    @Order(2)
    void courseMemberSeesPrivateMaterialsButCannotManage() throws Exception {
        mvc.perform(get("/api/v1/library").cookie(member)).andExpect(jsonPath("$.totalItems").value(3));
        mvc.perform(get("/api/v1/courses").cookie(member)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/courses/" + courseId + "/topics").cookie(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].materials.length()").value(3));
        mvc.perform(get("/api/v1/materials/" + privatePng + "/preview").cookie(member))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
        mvc.perform(get("/api/v1/materials/" + privateLink + "/download").cookie(member))
                .andExpect(status().isNotFound());

        mvc.perform(json(post("/api/v1/courses/" + courseId + "/topics").cookie(member),
                        "{\"title\":\"Intruso\",\"tool\":\"GENERAL\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/materials/" + publicPdf).cookie(member).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/courses/" + courseId + "/enrollments").cookie(member))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void studentOutsideTheCourseSeesOnlyPublicMaterials() throws Exception {
        mvc.perform(get("/api/v1/library").cookie(outsider)).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/v1/courses").cookie(outsider)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/courses/" + courseId + "/topics").cookie(outsider)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/materials/" + privatePng + "/preview").cookie(outsider))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    void uploadsAreCheckedByRealContentAndKind() throws Exception {
        upload(teacher, "Falso PDF", "GUIDE", "PUBLIC",
                new MockMultipartFile("file", "falso.pdf", "application/pdf", "MZ\u0090 no es un pdf".getBytes()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("El contenido del archivo no corresponde a un archivo .pdf válido."));
        upload(teacher, "Imagen como guía", "GUIDE", "PUBLIC",
                new MockMultipartFile("file", "diagrama.png", "image/png", PNG))
                .andExpect(status().isUnprocessableContent());
        upload(teacher, "Ejecutable", "TOOL_FILE", "PUBLIC",
                new MockMultipartFile("file", "programa.exe", "application/octet-stream", PNG))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(multipart("/api/v1/topics/" + topicId + "/materials").cookie(teacher).with(csrf())
                        .param("title", "Enlace raro").param("kind", "LINK").param("visibility", "PUBLIC")
                        .param("url", "javascript:alert(1)"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/v1/library").cookie(teacher)).andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    @Order(5)
    void ordinaryAdminCanPublishOrArchiveButNotCreate() throws Exception {
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/topics").cookie(ordinaryAdmin),
                        "{\"title\":\"No permitido\",\"tool\":\"GENERAL\"}"))
                .andExpect(status().isForbidden());
        upload(ordinaryAdmin, "No permitido", "GUIDE", "PUBLIC",
                new MockMultipartFile("file", "guia.pdf", "application/pdf", PDF))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/api/v1/materials/" + privatePng).cookie(ordinaryAdmin),
                        "{\"title\":\"Cambio\",\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(json(put("/api/v1/materials/" + privatePng + "/visibility").cookie(ordinaryAdmin),
                        "{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.canEdit").value(false))
                .andExpect(jsonPath("$.canModerate").value(true));
        mvc.perform(get("/api/v1/library")).andExpect(jsonPath("$.totalItems").value(2));

        mvc.perform(delete("/api/v1/materials/" + privatePng).cookie(ordinaryAdmin).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/library")).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/v1/materials/" + privatePng).cookie(teacher)).andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    void teacherOrganizesTopicsAndLibraryFilters() throws Exception {
        String second = JsonPath.read(mvc.perform(json(post("/api/v1/courses/" + courseId + "/topics").cookie(teacher),
                        "{\"title\":\"Primeros bloques\",\"tool\":\"SCRATCH\"}"))
                .andExpect(jsonPath("$.position").value(2))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(json(put("/api/v1/courses/" + courseId + "/topics/order").cookie(teacher),
                        "{\"ids\":[\"" + second + "\",\"" + topicId + "\"]}"))
                .andExpect(status().isNoContent());
        mvc.perform(json(put("/api/v1/courses/" + courseId + "/next-class").cookie(teacher),
                        "{\"topicId\":\"" + topicId + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(json(put("/api/v1/courses/" + courseId + "/next-class").cookie(teacher),
                        "{\"topicId\":\"" + second + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/courses/" + courseId + "/topics").cookie(member))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[0].nextClass").value(true))
                .andExpect(jsonPath("$[1].nextClass").value(false));

        mvc.perform(get("/api/v1/library").param("tool", "SCRATCH")).andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/v1/library").param("tool", "PSEINT").param("q", "VARIABLES"))
                .andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/v1/library").cookie(member).param("q", "pseint"))
                .andExpect(jsonPath("$.totalItems").value(2)); // por el título del tema y del enlace
        mvc.perform(get("/api/v1/library").cookie(member).param("kind", "LINK"))
                .andExpect(jsonPath("$.totalItems").value(1));

        mvc.perform(delete("/api/v1/topics/" + topicId).cookie(teacher).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/library")).andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/v1/materials/" + publicPdf)).andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void enrollmentsCanBeImportedListedAndRemoved() throws Exception {
        MockMultipartFile csv = new MockMultipartFile("file", "curso.csv", "text/csv",
                "cedula,nombre\n600001,Ya inscrita\n600010,Nuevo Estudiante\nxyz,Mala\n".getBytes());
        mvc.perform(multipart("/api/v1/courses/" + courseId + "/enrollments/import").file(csv).cookie(teacher)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(1))
                .andExpect(jsonPath("$.alreadyEnrolled").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1));
        // P-07: la misma lista desde Excel (.xlsx), con la cédula escrita como número.
        ByteArrayOutputStream xlsx = new ByteArrayOutputStream();
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            Row row = book.createSheet().createRow(0);
            row.createCell(0).setCellValue(600011d);
            row.createCell(1).setCellValue("Desde Excel");
            book.write(xlsx);
        }
        mvc.perform(multipart("/api/v1/courses/" + courseId + "/enrollments/import").cookie(teacher).with(csrf())
                        .file(new MockMultipartFile("file", "curso.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                xlsx.toByteArray())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(1));
        mvc.perform(get("/api/v1/courses/" + courseId + "/enrollments").cookie(teacher))
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.items[?(@.cedula == '600001')].registered").value(true));
        // Una cuenta Administrador también puede inscribir.
        mvc.perform(get("/api/v1/courses/" + courseId + "/enrollments").cookie(ordinaryAdmin))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/courses/" + courseId + "/enrollments/600001").cookie(teacher).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/courses/" + courseId + "/topics").cookie(member)).andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    void expiredFilesAreDeletedOnlyWhenNothingUsesThem() throws Exception {
        // Para este punto el PNG está archivado y el PDF quedó en un tema archivado.
        String topic = JsonPath.read(mvc.perform(json(post("/api/v1/courses/" + courseId + "/topics").cookie(teacher),
                        "{\"title\":\"Vigente\",\"tool\":\"GENERAL\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        topicId = topic;
        String active = id(upload(teacher, "Guía vigente", "GUIDE", "PUBLIC",
                new MockMultipartFile("file", "vigente.pdf", "application/pdf", PDF))
                .andExpect(status().isCreated()));

        assertThat(cleanup.deleteExpired()).as("nada vence antes de seis meses").isZero();
        jdbc.sql("update stored_file set retain_until = now() - interval '1 day'").update();

        assertThat(cleanup.deleteExpired()).isEqualTo(2);
        assertThat(cleanup.deleteExpired()).as("no repite").isZero();
        assertThat(jdbc.sql("select count(*) from stored_file where deleted_at is null").query(Long.class).single())
                .isEqualTo(1);
        try (var stored = Files.walk(storageDir)) {
            assertThat(stored.filter(Files::isRegularFile).count()).isEqualTo(1);
        }
        mvc.perform(get("/api/v1/materials/" + active + "/download")).andExpect(status().isOk());
    }

    @Test
    @Order(9)
    void searchesTreatSqlInjectionAsPlainText() throws Exception {
        List<String> payloads = List.of("' OR '1'='1", "' OR 1=1 --", "'; DROP TABLE user_account; --",
                "\" OR \"\"=\"", "%", "_", "\\", "') UNION SELECT cedula FROM user_account --");
        for (String payload : payloads) {
            mvc.perform(get("/api/v1/library").param("q", payload))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
            mvc.perform(get("/api/v1/forum/threads").param("q", payload))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
            mvc.perform(get("/api/v1/admin/users").param("q", payload).cookie(admin))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
            mvc.perform(get("/api/v1/materials/{id}", payload)).andExpect(status().is4xxClientError());
            mvc.perform(get("/api/v1/courses/{id}", payload).cookie(teacher)).andExpect(status().is4xxClientError());
        }
        // Nada se borró ni cambió: las tablas siguen ahí con sus datos.
        mvc.perform(get("/api/v1/library")).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/v1/admin/users").param("q", "600001").cookie(admin))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    @Order(10)
    void documentationOfTheApiIsOffByDefault() throws Exception {
        mvc.perform(get("/api/v1/openapi")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is4xxClientError());
    }

    /**
     * Entrar por URL con el rol equivocado: se recorren todos los endpoints de la API con la sesión de un estudiante
     * inscrito y de uno sin inscripción, con identificadores reales del curso. Solo responde con éxito lo de esta lista.
     */
    @Test
    @Order(11)
    void studentsCannotReachStaffEndpointsByUrl() throws Exception {
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/enrollments").cookie(teacher), "{\"cedula\":\"600001\"}"))
                .andExpect(status().isCreated());
        Set<String> anyStudent = Set.of("GET /api/v1/me", "GET /api/v1/courses", "GET /api/v1/library",
                "GET /api/v1/library/topics", "GET /api/v1/forum/threads", "GET /api/v1/system/info",
                "POST /api/v1/me/email-verification"); // reenviar la verificación de su propio correo
        Set<String> enrolled = new TreeSet<>(anyStudent);
        enrolled.addAll(Set.of("GET /api/v1/courses/{courseId}", "GET /api/v1/courses/{courseId}/topics",
                "GET /api/v1/courses/{courseId}/activities", "GET /api/v1/courses/{courseId}/grades/mine",
                "GET /api/v1/courses/{courseId}/gradebook"));
        assertThat(answeredBy(member)).as("estudiante inscrita").isEqualTo(enrolled);
        assertThat(answeredBy(outsider)).as("estudiante de otro curso").isEqualTo(new TreeSet<>(anyStudent));
    }

    /** Un docente solo ve las cédulas que él habilitó; la administración las ve todas (Ley 1581). */
    @Test
    @Order(12)
    void teachersSeeOnlyTheEnablementsTheyMade() throws Exception {
        mvc.perform(json(post("/api/v1/student-enablements").cookie(admin), "{\"cedula\":\"600009\"}"))
                .andExpect(status().isCreated());
        String byTeacher = mvc.perform(get("/api/v1/student-enablements").param("size", "100").cookie(teacher))
                .andReturn().getResponse().getContentAsString();
        String byAdmin = mvc.perform(get("/api/v1/student-enablements").param("size", "100").cookie(ordinaryAdmin))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(byTeacher, "$.items[*].cedula")).contains("600001").doesNotContain("600009");
        assertThat(JsonPath.<List<String>>read(byAdmin, "$.items[*].cedula")).contains("600001", "600009");
    }

    // ---------- utilidades ----------

    /** Endpoints que responden con éxito (2xx) a esta sesión; {courseId} y {topicId} son los reales del curso. */
    private Set<String> answeredBy(Cookie session) throws Exception {
        Set<String> answered = new TreeSet<>();
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.startsWith("/api/v1/") || pattern.startsWith("/api/v1/auth/")) {
                    continue;
                }
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                for (RequestMethod method : methods.isEmpty() ? Set.of(RequestMethod.GET) : methods) {
                    String uri = pattern.replace("{courseId}", courseId).replace("{topicId}", topicId)
                            .replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
                    int status = mvc.perform(request(HttpMethod.valueOf(method.name()), uri).cookie(session)
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                            .andReturn().getResponse().getStatus();
                    if (status < 300) {
                        answered.add(method + " " + pattern);
                    }
                }
            }
        }
        return answered;
    }

    private ResultActions upload(Cookie session, String title, String kind, String visibility,
            MockMultipartFile file) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/topics/" + topicId + "/materials");
        return mvc.perform(request.file(file).cookie(session).with(csrf())
                .param("title", title).param("kind", kind).param("visibility", visibility));
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
