package co.edu.unipamplona.logicode.grading.infrastructure.ai;

import java.net.http.HttpClient;
import java.time.Duration;

import co.edu.unipamplona.logicode.grading.application.AiGradingSettings;
import co.edu.unipamplona.logicode.grading.application.GradingProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class AiGradingConfig {

    /** Margen sobre el tiempo máximo de evaluación antes de considerar abandonado un trabajo en curso. */
    private static final Duration STALE_MARGIN = Duration.ofMinutes(2);

    @Bean
    AiGradingSettings aiGradingSettings(AiProperties properties) {
        return new AiGradingSettings(properties.enabled(), properties.maxAttempts(), properties.retryDelay(),
                properties.timeout().plus(STALE_MARGIN));
    }

    @Bean
    GradingProvider gradingProvider(AiProperties properties) {
        return new OllamaGradingAdapter(restClient(properties.timeout()), properties.ollamaUrl(), properties.model(), properties.keepAlive(),
                properties.contextTokens(), new ImagePreparer(properties.maxImageSide()));
    }

    static RestClient.Builder restClient(Duration timeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
