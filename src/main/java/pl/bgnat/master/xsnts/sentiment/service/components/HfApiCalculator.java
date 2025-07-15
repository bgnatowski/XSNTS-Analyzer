package pl.bgnat.master.xsnts.sentiment.service.components;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentScore;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HfApiCalculator implements SentimentCalculator {

    @Value("${app.sentiment.hf-url}")
    private String baseUrl;

    @Value("${app.sentiment.timeout-ms:3000}")
    private long timeoutMs;

    @Value("${app.sentiment.retry:0}")
    private int retryCount;

    private final WebClient.Builder webClientBuilder;

    @Override
    public SentimentScore evaluate(Collection<String> tokens) {

        String sentence = String.join(" ", tokens);

        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(64 * 1024))
                        .build())
                .build();

        try {
            HfResponse resp = client.post()
                    .uri("/sentiment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("text", sentence))
                    .retrieve()
                    .bodyToMono(HfResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retry(retryCount)
                    .block();

            if (resp == null) throw new IllegalStateException("Empty response");

            double val = resp.score();
            SentimentLabel label = switch (resp.sentiment()) {
                case "POS" -> SentimentLabel.POSITIVE;
                case "NEG" -> SentimentLabel.NEGATIVE;
                default     -> SentimentLabel.NEUTRAL;
            };
            return new SentimentScore(label, val); // pewność modelu co do podanej etykiety; 1 = model całkowicie przekonany, 0.5 ≈ ma wątpliwości

        } catch (Exception ex) {
            log.warn("HF-API call failed, fallback to NEUTRAL: {}", ex.getMessage());
            return new SentimentScore(SentimentLabel.NEUTRAL, 0.0);
        }
    }

    private record HfResponse(String sentiment, double score) {}
}
