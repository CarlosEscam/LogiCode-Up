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
    volatile int status = 200;
    volatile String response = "";
    volatile long delayMs = 0;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            lastRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
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

        JsonNode sent = JSON.readTree(lastRequest.get());
        assertThat(sent.path("model").asString()).isEqualTo("qwen2.5vl:7b");
        assertThat(sent.path("stream").booleanValue()).isFalse();
        assertThat(sent.path("options").path("temperature").intValue()).isZero();
        assertThat(sent.path("format").path("properties").path("criterios").path("minItems").intValue()).isEqualTo(2);
        JsonNode user = sent.path("messages").get(1);
        assertThat(user.path("images").size()).isEqualTo(1);
        String content = user.path("content").asString();
        assertThat(content).contains("Estudiante: Ana Estudiante", "Lectura (máximo 6 puntos)", "Archivo 1 (PSC)",
                "Se adjuntan 1 imagen");
        assertThat(lastRequest.get()).doesNotContain("1000000201").doesNotContain("@");
        BufferedImage sentImage = ImageIO.read(new java.io.ByteArrayInputStream(
                java.util.Base64.getDecoder().decode(user.path("images").get(0).asString())));
        assertThat(Math.max(sentImage.getWidth(), sentImage.getHeight())).isEqualTo(100);
    }

    @Test
    void illegibleAnswerMayOmitCriteria() {
        response = chat("""
                {"transcripcion":"","legible":false,"criterios":[],"retroalimentacion":"","confianza":20}""");
        GradingProvider.Result result = adapter(Duration.ofSeconds(5)).evaluate(request());
        assertThat(result.legible()).isFalse();
        assertThat(result.criteria()).isEmpty();
    }

    @Test
    void malformedAnswersAreTechnicalFailures() {
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
