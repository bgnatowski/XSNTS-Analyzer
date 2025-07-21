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
import pl.bgnat.master.xsnts.sentiment.service.factory.LexiconCalculator;
import pl.bgnat.master.xsnts.sentiment.service.factory.SentimentCalculator;
import pl.bgnat.master.xsnts.sentiment.service.factory.SentimentCalculatorFactory;
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

    private final ProcessedTweetRepository processedTweetRepository;
    private final SentimentResultRepository sentimentResultRepository;
    private final SentimentCalculatorFactory sentimentCalculatorFactory;
    private final ObjectMapper objectMapper;


    @Transactional
    public int analyzeAll(SentimentRequest request) {

        SentimentCalculator calculator = sentimentCalculatorFactory.choose(request.sentimentModelStrategy());
        TokenExtractor extractor = TokenExtractor.of(request.tokenStrategy());

        long total = processedTweetRepository.countWithoutSentiment(
                request.tokenStrategy(),
                request.sentimentModelStrategy());

        log.info("Do analizy: {} tweetów", total);

        int saved = 0;

        while (true) {
            var page = processedTweetRepository.findAllWithoutSentiment(
                    request.tokenStrategy(),
                    request.sentimentModelStrategy(),
                    PageRequest.of(0, pageSize));

            if (page.isEmpty()) break;

            List<SentimentResult> buffer = new ArrayList<>(flushBatch);

            for (ProcessedTweet tweet : page) {
                buildResult(tweet, extractor, calculator, request).ifPresent(buffer::add);

                if (buffer.size() == flushBatch) {
                    sentimentResultRepository.saveAll(buffer);
                    saved += buffer.size();
                    buffer.clear();
                }
            }
            if (!buffer.isEmpty()) {
                sentimentResultRepository.saveAll(buffer);
                saved += buffer.size();
            }
            log.info("Postęp: {}/{}", saved, total);
        }
        log.info("Analiza zakończona – wstawiono {} z {} rekordów", saved, total);
        return saved;
    }

    @Transactional
    public int deleteFromDb(SentimentRequest req) {
        return sentimentResultRepository.deleteByTokenStrategyAndSentimentModelStrategy(
                req.tokenStrategy(), req.sentimentModelStrategy());
    }

    private Optional<SentimentResult> buildResult(ProcessedTweet tweet,
                                                  TokenExtractor extractor,
                                                  SentimentCalculator calculator,
                                                  SentimentRequest request) {
        try {
            List<String> tokens = objectMapper.readValue(extractor.extract(tweet),
                    new TypeReference<>() {
                    });

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
