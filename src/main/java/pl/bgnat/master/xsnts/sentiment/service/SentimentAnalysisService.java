package pl.bgnat.master.xsnts.sentiment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentRequest;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;
import pl.bgnat.master.xsnts.sentiment.repository.SentimentResultRepository;
import pl.bgnat.master.xsnts.sentiment.service.components.SentimentCalculator;
import pl.bgnat.master.xsnts.sentiment.service.components.SentimentCalculatorFactory;
import pl.bgnat.master.xsnts.sentiment.service.components.TokenExtractor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentimentAnalysisService {

    @Value("${app.sentiment.flush-batch:5000}")
    private int flushBatch;

    @Value("${app.sentiment.page-size:10000}")
    private int pageSize;

    private final ProcessedTweetRepository   tweetRepo;
    private final SentimentResultRepository  resultRepo;
    private final SentimentCalculatorFactory calculatorFactory;
    private final ObjectMapper               mapper;

    @Transactional
    public int analyzeAll(SentimentRequest request) {

        SentimentCalculator calculator = calculatorFactory.choose(request.sentimentModelStrategy());
        TokenExtractor      extractor  = TokenExtractor.of(request.tokenStrategy());

        long total = tweetRepo.countWithoutSentiment(
                request.tokenStrategy(),
                request.sentimentModelStrategy());

        log.info("Do analizy: {} tweetów", total);

        int saved = 0;

        while (true) {
            var page = tweetRepo.findAllWithoutSentiment(
                    request.tokenStrategy(),
                    request.sentimentModelStrategy(),
                    PageRequest.of(0, pageSize));

            if (page.isEmpty()) break;

            List<SentimentResult> buffer = new ArrayList<>(flushBatch);

            for (ProcessedTweet tweet : page) {
                buildResult(tweet, extractor, calculator, request).ifPresent(buffer::add);

                if (buffer.size() == flushBatch) {
                    resultRepo.saveAll(buffer);
                    saved += buffer.size();
                    buffer.clear();
                }
            }
            if (!buffer.isEmpty()) {
                resultRepo.saveAll(buffer);
                saved += buffer.size();
            }
            log.info("Postęp: {}/{}", saved, total);
        }
        log.info("Analiza zakończona – wstawiono {} z {} rekordów", saved, total);
        return saved;
    }

    private Optional<SentimentResult> buildResult(ProcessedTweet tweet,
                                                  TokenExtractor extractor,
                                                  SentimentCalculator calculator,
                                                  SentimentRequest request) {
        try {
            List<String> tokens = mapper.readValue(extractor.extract(tweet),
                    new TypeReference<>() {});
            var score = calculator.evaluate(tokens);

            return Optional.of(SentimentResult.builder()
                    .processedTweet(tweet)
                    .tokenStrategy(request.tokenStrategy())
                    .sentimentModelStrategy(request.sentimentModelStrategy())
                    .label(score.label())
                    .score(score.value())
                    .analysisDate(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Tweet {} pominięty – {}", tweet.getId(), e.getMessage());
            return Optional.empty();
        }
    }
}
