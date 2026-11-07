package co.edu.unipamplona.logicode.grading.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import co.edu.unipamplona.logicode.grading.application.GradingProvider;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** El adaptador habla el protocolo de Ollama con un servidor simulado: pedido, respuesta y errores. */
class OllamaGradingAdapterTest {

    static final JsonMapper JSON = JsonMapper.builder().build();

    HttpServer server;
    final AtomicReference<String> lastRequest = new AtomicReference<>();
    final List<String> requests = new CopyOnWriteArrayList<>();
    volatile String readResponse = null;
    volatile int status = 200;
    volatile String response = "";
    volatile long delayMs = 0;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            String sent = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastRequest.set(sent);
            requests.add(sent);
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // El paso de lectura (sin rúbrica en el esquema) puede tener su propia respuesta.
            boolean reading = !sent.contains("\"criterios\"");
            byte[] body = (reading && readResponse != null ? readResponse : response).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsStructuredRequestWithImagesAndWithoutPersonalIdentifiers() throws Exception {
        response = chat("""
                {"transcripcion":"Leer a <- 0","legible":true,"criterios":[
                {"criterio":"Lectura","puntos":5.5,"comentario":"Bien"},{"criterio":"Cálculo","puntos":3,"comentario":"Falta"}],
                "retroalimentacion":"Vas bien.","confianza":87}""");
        GradingProvider.Result result = adapter(Duration.ofSeconds(5)).evaluate(request());

        assertThat(result.legible()).isTrue();
        assertThat(result.confidence()).isEqualTo(87);
        assertThat(result.transcription()).isEqualTo("Leer a <- 0");
        assertThat(result.feedback()).isEqualTo("Vas bien.");
        assertThat(result.criteria()).extracting(GradingProvider.CriterionResult::points)
                .containsExactly(new BigDecimal("5.5"), new BigDecimal("3"));

        // Dos pasos (T-107): 1) leer las imágenes sin enunciado, respuesta esperada ni rúbrica; 2) calificar el texto.
        assertThat(requests).hasSize(2);
        JsonNode read = JSON.readTree(requests.get(0));
        JsonNode sent = JSON.readTree(requests.get(1));
        assertThat(read.path("model").asString()).isEqualTo("qwen2.5vl:7b");
        assertThat(read.path("format").path("properties").has("criterios")).isFalse();
        JsonNode readUser = read.path("messages").get(1);
        assertThat(readUser.path("images").size()).isEqualTo(1);
        assertThat(requests.get(0)).doesNotContain("prom <- (a+b)/2").doesNotContain("Calcula el promedio")
                .doesNotContain("Lectura (máximo");
        assertThat(sent.path("stream").booleanValue()).isFalse();
        assertThat(sent.path("options").path("temperature").intValue()).isZero();
        assertThat(sent.path("format").path("properties").path("criterios").path("minItems").intValue()).isEqualTo(2);
        assertThat(sent.path("format").path("properties").has("transcripcion")).isFalse();
        JsonNode user = sent.path("messages").get(1);
        assertThat(user.has("images")).isFalse();
        String content = user.path("content").asString();
        assertThat(content).contains("Estudiante: Ana Estudiante", "Lectura (máximo 6 puntos)", "Archivo 1 (PSC)",
                "prom <- (a+b)/2", "Transcripción de la imagen de la entrega:\n<<<\nLeer a <- 0\n>>>");
        assertThat(requests).allSatisfy(r -> assertThat(r).doesNotContain("1000000201").doesNotContain("@"));
        JsonNode user0 = readUser;
        BufferedImage sentImage = ImageIO.read(new java.io.ByteArrayInputStream(
                java.util.Base64.getDecoder().decode(user0.path("images").get(0).asString())));
        assertThat(Math.max(sentImage.getWidth(), sentImage.getHeight())).isEqualTo(100);
    }

    @Test
    void illegibleAnswerMayOmitCriteria() {
        response = chat("""
                {"transcripcion":"","legible":false,"criterios":[],"retroalimentacion":"","confianza":20}""");
        GradingProvider.Result result = adapter(Duration.ofSeconds(5)).evaluate(request());
        assertThat(result.legible()).isFalse();
        assertThat(result.criteria()).isEmpty();
        assertThat(requests).as("si no se pudo leer, no se califica").hasSize(1);
    }

    @Test
    void submissionsWithoutImagesAreGradedInOneStep() {
        response = chat("""
                {"transcripcion":"Algoritmo x","legible":true,"criterios":[
                {"criterio":"Lectura","puntos":6,"comentario":"Bien"},{"criterio":"Cálculo","puntos":4,"comentario":"Bien"}],
                "retroalimentacion":"Bien.","confianza":90}""");
        GradingProvider.Request request = request();
        GradingProvider.Result result = adapter(Duration.ofSeconds(5)).evaluate(new GradingProvider.Request(
                request.studentName(), request.activityTitle(), request.statement(), request.expectedAnswer(),
                request.criteria(), request.textAnswer(), request.textFiles(), List.of()));
        assertThat(result.confidence()).isEqualTo(90);
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0)).contains("prom <- (a+b)/2", "Algoritmo x");
    }

    @Test
    void theGradingStepOnlySeesWhatTheReadingStepRead() throws Exception {
        readResponse = chat("""
                {"transcripcion":"Leer a [ilegible]","legible":true}""");
        response = chat("""
                {"criterios":[{"criterio":"Lectura","puntos":1,"comentario":"Incompleto"},
                {"criterio":"Cálculo","puntos":0,"comentario":"No se ve"}],"retroalimentacion":"Revisa.","confianza":40}""");
        GradingProvider.Result result = adapter(Duration.ofSeconds(5)).evaluate(request());
        assertThat(result.transcription()).isEqualTo("Leer a [ilegible]");
        assertThat(result.confidence()).isEqualTo(40);
        assertThat(JSON.readTree(requests.get(1)).path("messages").get(1).path("content").asString())
                .contains("<<<\nLeer a [ilegible]\n>>>");
    }

    @Test
    void malformedAnswersAreTechnicalFailures() {
        readResponse = chat("{\"legible\":true,\"transcripcion\":\"Leer a\"}");
        response = chat("no es json");
        assertThatThrownBy(() -> adapter(Duration.ofSeconds(5)).evaluate(request()))
                .isInstanceOf(GradingProvider.GradingFailure.class).hasMessageContaining("no es JSON");
        response = chat("{\"legible\":true,\"criterios\":[],\"confianza\":0.9}");
        assertThatThrownBy(() -> adapter(Duration.ofSeconds(5)).evaluate(request()))
                .isInstanceOf(GradingProvider.GradingFailure.class).hasMessageContaining("confianza");
        response = chat("{\"legible\":true,\"criterios\":[{\"puntos\":1}],\"confianza\":90}");
        assertThatThrownBy(() -> adapter(Duration.ofSeconds(5)).evaluate(request()))
                .isInstanceOf(GradingProvider.GradingFailure.class).hasMessageContaining("rúbrica tiene 2");
    }

    @Test
    void providerErrorsAndTimeoutsAreTechnicalFailures() {
        status = 404;
        response = "{\"error\":\"model 'qwen2.5vl:7b' not found\"}";
        assertThatThrownBy(() -> adapter(Duration.ofSeconds(5)).evaluate(request()))
                .isInstanceOf(GradingProvider.GradingFailure.class)
                .hasMessage("El servicio de IA respondió 404: model 'qwen2.5vl:7b' not found");
        status = 500;
        response = "{\"error\":\"{\\\"error\\\":{\\\"code\\\":500,\\\"message\\\":\\\"image input is not supported\\\"}}\"}";
        assertThatThrownBy(() -> adapter(Duration.ofSeconds(5)).evaluate(request()))
                .hasMessage("El servicio de IA respondió 500: image input is not supported");

        status = 200;
        response = chat("{}");
        delayMs = 1500;
        assertThatThrownBy(() -> adapter(Duration.ofMillis(300)).evaluate(request()))
                .isInstanceOf(GradingProvider.GradingFailure.class)
                .hasMessageContaining("no respondió a tiempo");
    }

    @Test
    void aReadingThatRunsAwayCountsAsIllegibleWithoutRetrying() throws Exception {
        // Verificación de la 1.3.0 en el PC de Carlos: con S09 y S10 el modelo se repitió hasta cortarse.
        readResponse = chat("{\"legible\":true,\"transcripcion\":\"Algoritmo Algoritmo Algoritmo");
        GradingProvider.Result result = adapter(Duration.ofSeconds(5)).evaluate(request());
        assertThat(result.legible()).isFalse();
        assertThat(result.criteria()).isEmpty();
        assertThat(result.transcription()).startsWith("[No se pudo leer:");
        assertThat(requests).hasSize(1);

        requests.clear();
        readResponse = null;
        status = 500;
        response = "{\"error\":\"prediction aborted, token repeat limit reached\"}";
        assertThat(adapter(Duration.ofSeconds(5)).evaluate(request()).legible()).isFalse();
        assertThat(JSON.readTree(requests.get(0)).path("options").path("num_predict").intValue())
                .isEqualTo(OllamaGradingAdapter.MAX_READ_TOKENS);
    }

    private OllamaGradingAdapter adapter(Duration timeout) {
        return new OllamaGradingAdapter(AiGradingConfig.restClient(timeout),
                "http://127.0.0.1:" + server.getAddress().getPort(), "qwen2.5vl:7b", "5m", 8192,
                new ImagePreparer(100));
    }

    private static String chat(String content) {
        return JSON.writeValueAsString(java.util.Map.of("model", "qwen2.5vl:7b", "done", true,
                "message", java.util.Map.of("role", "assistant", "content", content)));
    }

    private static GradingProvider.Request request() {
        return new GradingProvider.Request("Ana Estudiante", "Promedio", "Calcula el promedio.", "prom <- (a+b)/2",
                List.of(new GradingProvider.Criterion("Lectura", "Lee los datos", new BigDecimal("6.00")),
                        new GradingProvider.Criterion("Cálculo", null, new BigDecimal("4"))),
                "Mi respuesta", List.of(new GradingProvider.TextFile("Archivo 1", "PSC", "Algoritmo x")),
                List.of(new GradingProvider.Image("Imagen 1", png(400, 300))));
    }

    static byte[] png(int width, int height) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
