package co.edu.unipamplona.logicode.quizzes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import co.edu.unipamplona.logicode.quizzes.application.TakeQuizzes;
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
 * Etapa 7 contra PostgreSQL real con un reloj que se adelanta: banco de preguntas, quiz calificable con tiempo total,
 * intentos, envío automático al agotarse el tiempo, ranking y revisión; quiz de repaso; calificación determinista de
 * PSeInt con casos de prueba y el intérprete con sus límites.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "logicode.identity.initial-admins[0].cedula=900001",
        "logicode.identity.initial-admins[0].full-name=Administradora Uno",
        "logicode.identity.initial-admins[0].email=admin1@example.com",
        "logicode.quizzes.sweeper-enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuizAndPseintFlowsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    static final List<EmailMessage> OUTBOX = new CopyOnWriteArrayList<>();
    static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    static final String PASSWORD = "Clave#2026";
    static final MovableClock CLOCK = new MovableClock(Instant.now().truncatedTo(ChronoUnit.SECONDS));

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        Path dir = Files.createTempDirectory("logicode-quiz-it");
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

    @Autowired
    TakeQuizzes takeQuizzes;

    Cookie admin;
    Cookie ordinaryAdmin;
    Cookie teacher;
    Cookie ana;
    Cookie beto;
    String betoId;
    String courseId;
    String quizId;
    String practiceId;
    String single;
    String multiple;
    String trueFalse;
    String shortAnswer;

    @BeforeAll
    void createPeopleAndCourse() throws Exception {
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
        betoId = userId("600004");

        mvc.perform(json(post("/api/v1/student-enablements").cookie(teacher), "{\"cedula\":\"600005\"}"))
                .andExpect(status().isCreated());
        register("student-registrations", "600005", "Admin Ordinario", "ordinario@example.com");
        mvc.perform(json(patch("/api/v1/admin/users/" + userId("600005") + "/role").cookie(admin),
                        "{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk());
        ordinaryAdmin = login("600005");
    }

    @Test
    @Order(1)
    void teacherBuildsTheQuestionBank() throws Exception {
        String bank = "/api/v1/courses/" + courseId + "/questions";
        single = id(mvc.perform(json(post(bank).cookie(teacher), "{\"kind\":\"SINGLE\",\"prompt\":\"¿Qué ciclo "
                        + "evalúa la condición al final?\",\"points\":2,\"explanation\":\"Repetir revisa al final.\","
                        + "\"options\":[{\"text\":\"Mientras\",\"correct\":false},{\"text\":\"Repetir\",\"correct\":true}]}"))
                .andExpect(status().isCreated()));
        multiple = id(mvc.perform(json(post(bank).cookie(teacher), "{\"kind\":\"MULTIPLE\",\"prompt\":\"¿Cuáles son "
                        + "ciclos?\",\"options\":[{\"text\":\"Para\",\"correct\":true},{\"text\":\"Si\",\"correct\":false},"
                        + "{\"text\":\"Mientras\",\"correct\":true}]}"))
                .andExpect(status().isCreated()));
        trueFalse = id(mvc.perform(json(post(bank).cookie(teacher), "{\"kind\":\"TRUE_FALSE\",\"prompt\":\"Un "
                        + "algoritmo es finito.\",\"options\":[{\"text\":\"x\",\"correct\":true},{\"text\":\"y\",\"correct\":false}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options[0].text").value("Verdadero")));
        shortAnswer = id(mvc.perform(json(post(bank).cookie(teacher), "{\"kind\":\"SHORT\",\"prompt\":\"¿Qué "
                        + "instrucción muestra datos?\",\"acceptedAnswers\":[\"Escribir\",\"Imprimir\"]}"))
                .andExpect(status().isCreated()));

        mvc.perform(json(post(bank).cookie(teacher), "{\"kind\":\"SINGLE\",\"prompt\":\"Mal\",\"options\":["
                        + "{\"text\":\"a\",\"correct\":true},{\"text\":\"b\",\"correct\":true}]}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(json(post(bank).cookie(ordinaryAdmin), "{\"kind\":\"SHORT\",\"prompt\":\"x\","
                        + "\"acceptedAnswers\":[\"x\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(bank).cookie(ordinaryAdmin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get(bank).cookie(ana)).andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    void gradedQuizNeedsQuestionsToPublishAndCannotReceiveFiles() throws Exception {
        quizId = id(mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Quiz de ciclos", "QUIZ", "20", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acceptsText").value(false))
                .andExpect(jsonPath("$.maxAttempts").value(1)));
        mvc.perform(post("/api/v1/activities/" + quizId + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(json(put("/api/v1/activities/" + quizId + "/rubric").cookie(teacher),
                        "{\"criteria\":[{\"name\":\"x\",\"maxPoints\":1}]}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(json(put(quiz(quizId)).cookie(ordinaryAdmin), quizBody(10, single)))
                .andExpect(status().isNotFound());
        mvc.perform(json(put(quiz(quizId)).cookie(teacher), quizBody(10, single, multiple, trueFalse, shortAnswer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxPoints").value(5));
        mvc.perform(get("/api/v1/activities/" + quizId + "/rubric").cookie(teacher))
                .andExpect(jsonPath("$.criteria.length()").value(4))
                .andExpect(jsonPath("$.expectedAnswer").value(org.hamcrest.Matchers.containsString("2. Para + Mientras")));
        mvc.perform(post("/api/v1/activities/" + quizId + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isOk());

        mvc.perform(multipart("/api/v1/activities/" + quizId + "/submissions").param("text", "respuesta")
                        .cookie(ana).with(csrf()))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get(quiz(quizId)).cookie(ana))
                .andExpect(jsonPath("$.questionCount").value(4))
                .andExpect(jsonPath("$.questions").doesNotExist());
    }

    @Test
    @Order(3)
    void studentAnswersAndGetsAnAutomaticGradeWithoutSeeingTheKey() throws Exception {
        String attempt = id(mvc.perform(post(attempts(quizId)).cookie(ana).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.deadline").value(CLOCK.instant().plus(Duration.ofMinutes(10)).toString()))
                .andExpect(jsonPath("$.questions[0].options[1]").value("Repetir"))
                .andExpect(jsonPath("$.questions[0].correctChoices").doesNotExist()));
        mvc.perform(post(attempts(quizId)).cookie(ana).with(csrf()))
                .andExpect(jsonPath("$.id").value(attempt));
        mvc.perform(json(put("/api/v1/quiz-attempts/" + attempt + "/answers").cookie(ana),
                        answers(answer(single, "[1]"), answer(multiple, "[0,2]"))))
                .andExpect(status().isNoContent());
        mvc.perform(json(put("/api/v1/quiz-attempts/" + attempt + "/answers").cookie(beto),
                        answers(answer(single, "[0]"))))
                .andExpect(status().isNotFound());
        mvc.perform(json(put("/api/v1/quiz-attempts/" + attempt + "/answers").cookie(ana),
                        answers(answer(single, "[7]"))))
                .andExpect(status().isUnprocessableContent());

        CLOCK.advance(Duration.ofMinutes(4));
        String submission = JsonPath.read(mvc.perform(json(post("/api/v1/quiz-attempts/" + attempt + "/finish")
                                .cookie(ana), answers(answer(single, "[1]"), answer(multiple, "[0,2]"),
                                answer(trueFalse, "[1]"), "{\"questionId\":\"" + shortAnswer + "\",\"text\":\" ESCRIBIR \"}")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("FINISHED"))
                        .andExpect(jsonPath("$.correctCount").value(3))
                        .andExpect(jsonPath("$.points").value(4))
                        .andExpect(jsonPath("$.score").value(4.0))
                        .andExpect(jsonPath("$.durationSeconds").value(240))
                        .andExpect(jsonPath("$.reviewAvailable").value(false))
                        .andExpect(jsonPath("$.questions[2].correct").doesNotExist())
                        .andReturn().getResponse().getContentAsString(), "$.submissionId");

        mvc.perform(get("/api/v1/submissions/" + submission + "/grade").cookie(ana))
                .andExpect(jsonPath("$.grade.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.grade.score").value(4.0))
                .andExpect(jsonPath("$.grade.source").value("AUTOMATIC"))
                .andExpect(jsonPath("$.grade.feedback").value("Respuestas correctas: 3 de 4 (4 de 5 puntos)."));
        mvc.perform(get("/api/v1/activities/" + quizId).cookie(ana))
                .andExpect(jsonPath("$.progress.state").value("SUBMITTED"))
                .andExpect(jsonPath("$.progress.attemptsUsed").value(1));
        mvc.perform(post(attempts(quizId)).cookie(ana).with(csrf()))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(json(put(quiz(quizId)).cookie(teacher), quizBody(5, single)))
                .andExpect(status().isConflict());
        mvc.perform(json(put("/api/v1/questions/" + single).cookie(teacher), "{\"kind\":\"SHORT\",\"prompt\":\"x\","
                        + "\"acceptedAnswers\":[\"x\"]}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/questions/" + single).cookie(teacher).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    void anAbandonedAttemptIsSentWhenTimeRunsOutAndTheTeacherCanGrantAnother() throws Exception {
        String attempt = id(mvc.perform(post(attempts(quizId)).cookie(beto).with(csrf())).andExpect(status().isOk()));
        mvc.perform(json(put("/api/v1/quiz-attempts/" + attempt + "/answers").cookie(beto),
                        answers(answer(single, "[1]"), answer(multiple, "[0,1,2]"), answer(trueFalse, "[0]"))))
                .andExpect(status().isNoContent());

        CLOCK.advance(Duration.ofMinutes(10).plusSeconds(10));
        assertThat(takeQuizzes.finishExpired()).as("aún dentro del margen").isZero();
        CLOCK.advance(Duration.ofSeconds(30));
        assertThat(takeQuizzes.finishExpired()).isEqualTo(1);
        mvc.perform(get("/api/v1/quiz-attempts/" + attempt).cookie(beto))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.durationSeconds").value(600))
                .andExpect(jsonPath("$.score").value(3.0));
        mvc.perform(json(post("/api/v1/quiz-attempts/" + attempt + "/finish").cookie(beto),
                        answers(answer(single, "[0]"))))
                .andExpect(jsonPath("$.score").value(3.0));

        mvc.perform(post(attempts(quizId)).cookie(beto).with(csrf())).andExpect(status().isUnprocessableContent());
        mvc.perform(json(put("/api/v1/activities/" + quizId + "/exceptions/" + betoId).cookie(teacher),
                        "{\"extraAttempts\":1}"))
                .andExpect(status().isNoContent());
        String second = id(mvc.perform(post(attempts(quizId)).cookie(beto).with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(2)));
        CLOCK.advance(Duration.ofMinutes(2));
        mvc.perform(json(post("/api/v1/quiz-attempts/" + second + "/finish").cookie(beto),
                        answers(answer(single, "[1]"), answer(multiple, "[0,2]"), answer(trueFalse, "[0]"),
                                "{\"questionId\":\"" + shortAnswer + "\",\"text\":\"imprimir\"}")))
                .andExpect(jsonPath("$.score").value(5.0));

        mvc.perform(get(ranking(quizId)).cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Beto Estudiante"))
                .andExpect(jsonPath("$[0].score").value(5.0))
                .andExpect(jsonPath("$[0].durationSeconds").value(120))
                .andExpect(jsonPath("$[0].attemptId").doesNotExist())
                .andExpect(jsonPath("$[1].mine").value(true));
        mvc.perform(get(ranking(quizId)).cookie(teacher)).andExpect(jsonPath("$[0].attemptId").value(second));
        mvc.perform(get("/api/v1/courses/" + courseId + "/review-queue").cookie(teacher))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(5)
    void gradedReviewOpensWhenTheQuizCloses() throws Exception {
        String attempt = JsonPath.read(mvc.perform(get(attempts(quizId) + "/mine").cookie(ana))
                .andExpect(jsonPath("$[0].reviewAvailable").value(false))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        CLOCK.advance(Duration.ofDays(3));
        mvc.perform(get("/api/v1/quiz-attempts/" + attempt).cookie(ana))
                .andExpect(jsonPath("$.reviewAvailable").value(true))
                .andExpect(jsonPath("$.questions[0].correctChoices[0]").value(1))
                .andExpect(jsonPath("$.questions[0].explanation").value("Repetir revisa al final."))
                .andExpect(jsonPath("$.questions[2].correct").value(false))
                .andExpect(jsonPath("$.questions[3].acceptedAnswers[1]").value("Imprimir"));
    }

    @Test
    @Order(6)
    void practiceQuizHasNoGradeAndShowsTheReviewRightAway() throws Exception {
        mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Repaso", "PRACTICE_QUIZ", "10", 1)))
                .andExpect(status().isUnprocessableContent());
        practiceId = id(mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Repaso de ciclos", "PRACTICE_QUIZ", null, 1)))
                .andExpect(status().isCreated()));
        // P-04: el repaso muestra las respuestas al terminar, así que no puede usar preguntas del quiz calificable.
        mvc.perform(json(put(quiz(practiceId)).cookie(teacher), quizBody(5, trueFalse, shortAnswer)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("quiz calificable")));
        String bank = "/api/v1/courses/" + courseId + "/questions";
        String practiceTrueFalse = id(mvc.perform(json(post(bank).cookie(teacher), "{\"kind\":\"TRUE_FALSE\","
                        + "\"prompt\":\"Mientras puede no ejecutarse nunca.\",\"options\":[{\"text\":\"x\",\"correct\":true},"
                        + "{\"text\":\"y\",\"correct\":false}]}"))
                .andExpect(status().isCreated()));
        String practiceShort = id(mvc.perform(json(post(bank).cookie(teacher), "{\"kind\":\"SHORT\",\"prompt\":\"¿Qué "
                        + "instrucción lee datos?\",\"acceptedAnswers\":[\"Leer\"]}"))
                .andExpect(status().isCreated()));
        mvc.perform(json(put(quiz(practiceId)).cookie(teacher), quizBody(5, practiceTrueFalse, practiceShort)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/activities/" + practiceId + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isOk());

        for (int i = 0; i < 2; i++) {
            String attempt = id(mvc.perform(post(attempts(practiceId)).cookie(ana).with(csrf()))
                    .andExpect(status().isOk()));
            CLOCK.advance(Duration.ofSeconds(30 + i * 30));
            mvc.perform(json(post("/api/v1/quiz-attempts/" + attempt + "/finish").cookie(ana),
                            answers(answer(practiceTrueFalse, i == 0 ? "[1]" : "[0]"))))
                    .andExpect(jsonPath("$.reviewAvailable").value(true))
                    .andExpect(jsonPath("$.submissionId").doesNotExist())
                    .andExpect(jsonPath("$.questions[0].correctChoices[0]").value(0));
        }
        mvc.perform(get(ranking(practiceId)).cookie(ana))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].score").value(2.5));
        mvc.perform(get("/api/v1/activities/" + practiceId + "/submissions").cookie(teacher))
                .andExpect(jsonPath("$[?(@.fullName == 'Ana Estudiante')].attempts").value(0));
        CLOCK.advance(Duration.ofDays(3));
        mvc.perform(get("/api/v1/activities/" + practiceId).cookie(ana))
                .andExpect(jsonPath("$.progress.state").value("CLOSED"));
        mvc.perform(post(attempts(practiceId)).cookie(ana).with(csrf())).andExpect(status().isUnprocessableContent());
    }

    @Test
    @Order(7)
    void pseintActivityIsGradedByRunningTheTestCases() throws Exception {
        String activityId = id(mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        "{\"title\":\"Par o impar\",\"kind\":\"TASK\",\"statement\":\"Lee un número y di si es par.\","
                                + "\"acceptsText\":true,\"acceptsFiles\":true,\"acceptsPhotos\":false,"
                                + "\"fileGroups\":[\"TOOL_FILES\"],\"opensAt\":\"" + CLOCK.instant().minusSeconds(60)
                                + "\",\"closesAt\":\"" + CLOCK.instant().plus(Duration.ofDays(2))
                                + "\",\"maxAttempts\":5}"))
                .andExpect(status().isCreated()));
        mvc.perform(json(put("/api/v1/activities/" + activityId + "/rubric").cookie(teacher),
                        "{\"criteria\":[{\"name\":\"Funciona con los casos\",\"maxPoints\":5}],\"testCases\":["
                                + "{\"input\":\"4\",\"expectedOutput\":\"par\"},"
                                + "{\"input\":\"7\",\"expectedOutput\":\"impar\"},"
                                + "{\"input\":\"10\",\"expectedOutput\":\"par\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCaseCount").value(3));
        mvc.perform(post("/api/v1/activities/" + activityId + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/activities/" + activityId + "/rubric").cookie(ana))
                .andExpect(jsonPath("$.testCaseCount").value(3))
                .andExpect(jsonPath("$.testCases").doesNotExist());

        String good = submit(activityId, ana, "Algoritmo P\n Escribir \"Número:\"\n Leer n\n Si n % 2 = 0 Entonces\n"
                + "  Escribir \"par\"\n SiNo\n  Escribir \"impar\"\n FinSi\nFinAlgoritmo");
        mvc.perform(get("/api/v1/submissions/" + good + "/grade").cookie(ana))
                .andExpect(jsonPath("$.grade.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.grade.score").value(5.0))
                .andExpect(jsonPath("$.grade.source").value("AUTOMATIC"))
                .andExpect(jsonPath("$.grade.feedback").value(org.hamcrest.Matchers.startsWith(
                        "Casos de prueba aprobados: 3 de 3.")));

        String alwaysPar = id(mvc.perform(multipart("/api/v1/activities/" + activityId + "/submissions")
                        .file(new MockMultipartFile("files", "par.psc", "text/plain",
                                "Proceso P\n Leer n\n Escribir \"par\"\nFinProceso\n".getBytes()))
                        .cookie(beto).with(csrf()))
                .andExpect(status().isCreated()));
        mvc.perform(get("/api/v1/submissions/" + alwaysPar + "/grade").cookie(beto))
                .andExpect(jsonPath("$.grade.score").value(3.3))
                .andExpect(jsonPath("$.grade.feedback").value(org.hamcrest.Matchers.containsString(
                        "Caso 2: la salida no es la esperada.")));
        mvc.perform(post("/api/v1/submissions/" + alwaysPar + "/test-run").cookie(teacher).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(2))
                .andExpect(jsonPath("$.cases[1].expectedOutput").value("impar"))
                .andExpect(jsonPath("$.cases[1].output").value("par\n"));
        mvc.perform(post("/api/v1/submissions/" + alwaysPar + "/test-run").cookie(beto).with(csrf()))
                .andExpect(status().isForbidden());

        String broken = submit(activityId, ana, "Si n es par escribo par");
        mvc.perform(get("/api/v1/submissions/" + broken + "/grade").cookie(teacher))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.reviewReason").value("CODE_ERROR"))
                .andExpect(jsonPath("$.canRunTests").value(true));
        mvc.perform(post("/api/v1/submissions/" + broken + "/test-run").cookie(teacher).with(csrf()))
                .andExpect(jsonPath("$.programError").value(org.hamcrest.Matchers.startsWith("Línea 1:")));
    }

    @Test
    @Order(8)
    void blockChallengeIsGradedByRunningTheStudentsProgram() throws Exception {
        String challenge = "\"blockChallenge\":{\"grid\":[\"S..#\",\"##.#\",\"##.M\"],\"facing\":\"E\","
                + "\"maxBlocks\":7,\"allowed\":[\"avanzar\",\"izquierda\",\"derecha\"],"
                + "\"start\":[{\"type\":\"avanzar\"},{\"type\":\"izquierda\"}]}";
        String rubric = "{\"criteria\":[{\"name\":\"Llega a la meta\",\"maxPoints\":5}]," + challenge + "}";
        String photosOnly = id(mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Reto sin texto", "TASK", "null", 1).replace("\"acceptsText\":true",
                                "\"acceptsText\":false").replace("\"acceptsPhotos\":false", "\"acceptsPhotos\":true")))
                .andExpect(status().isCreated()));
        mvc.perform(json(put("/api/v1/activities/" + photosOnly + "/rubric").cookie(teacher), rubric))
                .andExpect(status().isUnprocessableEntity());

        String activityId = id(mvc.perform(json(post("/api/v1/courses/" + courseId + "/activities").cookie(teacher),
                        activity("Reto: doblar la esquina", "TASK", "null", 3)))
                .andExpect(status().isCreated()));
        mvc.perform(json(put("/api/v1/activities/" + activityId + "/rubric").cookie(teacher),
                        rubric.replace("[\"S..#\",", "[\"S..#\",\"S...\",")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("una salida")));
        mvc.perform(json(put("/api/v1/activities/" + activityId + "/rubric").cookie(teacher),
                        rubric.replace("]," + challenge, "],\"testCases\":[{\"expectedOutput\":\"x\"}]," + challenge)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(json(put("/api/v1/activities/" + activityId + "/rubric").cookie(teacher), rubric))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/activities/" + activityId + "/publish").cookie(teacher).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/activities/" + activityId + "/rubric").cookie(ana))
                .andExpect(jsonPath("$.blockChallenge.grid[2]").value("##.M"))
                .andExpect(jsonPath("$.blockChallenge.start[1].type").value("izquierda"));

        String a = "{\"type\":\"avanzar\",\"id\":\"b1\"}";
        String right = "{\"type\":\"derecha\"}";
        String left = "{\"type\":\"izquierda\"}";
        String solved = submit(activityId, ana, "[" + String.join(",", a, a, right, a, a, left, a) + "]");
        mvc.perform(get("/api/v1/submissions/" + solved + "/grade").cookie(ana))
                .andExpect(jsonPath("$.grade.score").value(5.0))
                .andExpect(jsonPath("$.grade.source").value("AUTOMATIC"))
                .andExpect(jsonPath("$.grade.feedback").value("Reto superado: el robot llegó a la meta con 7 bloques."));

        String crashed = submit(activityId, beto, "[" + String.join(",", a, a, a) + "]");
        mvc.perform(get("/api/v1/submissions/" + crashed + "/grade").cookie(beto))
                .andExpect(jsonPath("$.grade.score").value(0.0))
                .andExpect(jsonPath("$.grade.feedback").value(org.hamcrest.Matchers.startsWith("El robot chocó")));

        String loop = submit(activityId, beto, "[{\"type\":\"repetir\",\"times\":4,\"body\":[" + a + "]}]");
        mvc.perform(get("/api/v1/submissions/" + loop + "/grade").cookie(teacher))
                .andExpect(jsonPath("$.grade.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.grade.reviewReason").value("CODE_ERROR"));
        String text = submit(activityId, beto, "avanzo dos y giro");
        mvc.perform(get("/api/v1/submissions/" + text + "/grade").cookie(teacher))
                .andExpect(jsonPath("$.grade.reviewReason").value("CODE_ERROR"));
    }

    @Test
    @Order(9)
    void editorRunsProgramsWithinLimitsAndNeedsASession() throws Exception {
        mvc.perform(json(post("/api/v1/pseint/runs").cookie(ana),
                        "{\"code\":\"Algoritmo A\\n Leer x\\n Escribir x * 2\\nFinAlgoritmo\",\"input\":\"21\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.output").value("42\n"));
        mvc.perform(json(post("/api/v1/pseint/runs").cookie(ana),
                        "{\"code\":\"Algoritmo A\\n Mientras Verdadero\\n FinMientras\\nFinAlgoritmo\"}"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.errorKind").value("LIMIT"));
        mvc.perform(json(post("/api/v1/pseint/runs"), "{\"code\":\"Algoritmo A\\nFinAlgoritmo\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String activity(String title, String kind, String weight, int attempts) {
        return "{\"title\":\"" + title + "\",\"kind\":\"" + kind + "\",\"statement\":\"Responde las preguntas.\","
                + "\"acceptsText\":true,\"acceptsFiles\":false,\"acceptsPhotos\":false,"
                + "\"weightPercent\":" + weight + ",\"opensAt\":\"" + CLOCK.instant().minusSeconds(60)
                + "\",\"closesAt\":\"" + CLOCK.instant().plus(Duration.ofDays(2)) + "\",\"maxAttempts\":" + attempts
                + "}";
    }

    private static String quizBody(int minutes, String... questionIds) {
        return "{\"timeLimitMinutes\":" + minutes + ",\"questionIds\":[\"" + String.join("\",\"", questionIds) + "\"]}";
    }

    private static String answer(String questionId, String choices) {
        return "{\"questionId\":\"" + questionId + "\",\"choices\":" + choices + "}";
    }

    private static String answers(String... answers) {
        return "{\"answers\":[" + String.join(",", answers) + "]}";
    }

    private static String quiz(String activityId) {
        return "/api/v1/activities/" + activityId + "/quiz";
    }

    private static String attempts(String activityId) {
        return quiz(activityId) + "/attempts";
    }

    private static String ranking(String activityId) {
        return quiz(activityId) + "/ranking";
    }

    private String submit(String activityId, Cookie student, String text) throws Exception {
        return id(mvc.perform(multipart("/api/v1/activities/" + activityId + "/submissions").param("text", text)
                        .cookie(student).with(csrf()))
                .andExpect(status().isCreated()));
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
