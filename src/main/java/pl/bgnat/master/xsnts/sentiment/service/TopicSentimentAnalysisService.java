package pl.bgnat.master.xsnts.sentiment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.sentiment.dto.*;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;
import pl.bgnat.master.xsnts.sentiment.model.TopicSentimentStatsEntity;
import pl.bgnat.master.xsnts.sentiment.repository.SentimentResultRepository;
import pl.bgnat.master.xsnts.sentiment.repository.TopicSentimentStatsRepository;
import pl.bgnat.master.xsnts.topicmodeling.model.DocumentTopicAssignment;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicResult;
import pl.bgnat.master.xsnts.topicmodeling.repository.DocumentTopicAssignmentRepository;
import pl.bgnat.master.xsnts.topicmodeling.repository.TopicResultRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicSentimentAnalysisService {

    private final DocumentTopicAssignmentRepository documentTopicAssignmentRepository;
    private final TopicResultRepository topicResultRepository;
    private final ProcessedTweetRepository processedTweetRepository;
    private final SentimentResultRepository sentimentResultRepository;
    private final TopicSentimentStatsRepository topicSentimentStatsRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<TopicSentimentStats> getSentimentStatsForModel(
            Long modelId,
            SentimentRequest request) {

        Map<Long, Integer> processedIdToTopic = buildProcessedIdTopicMap(modelId);
        if (processedIdToTopic.isEmpty()) {
            log.warn("Model {} nie posiada tweetów z przypisanym tematem", modelId);
            return List.of();
        }

        // 1. Pobranie wyników sentymentu dla danej strategii
        log.info("Pobieranie wyników dla: {} / {} po topicId", request.tokenStrategy(), request.sentimentModelStrategy());
        List<SentimentResult> results = sentimentResultRepository
                .findAllByProcessedTweetIdInAndTokenStrategyAndSentimentModelStrategy(
                        processedIdToTopic.keySet(),
                        request.tokenStrategy(),
                        request.sentimentModelStrategy());

        if (results.isEmpty()) {
            log.warn("Brak wyników sentymentu dla modelu {} i strategii {}", modelId,
                    request.sentimentModelStrategy());
            return List.of();
        }

        log.info("Grupowanie po topicId");
        Map<Integer, List<SentimentResult>> grouped = results.stream()
                .collect(Collectors.groupingBy(
                        r -> processedIdToTopic.get(r.getProcessedTweet().getId())
                ));

        log.info("Tworzenie etykiet tematów");
        Map<Integer, String> topicLabels = topicResultRepository
                .findByTopicModelingResultId(modelId).stream()
                .collect(Collectors.toMap(TopicResult::getTopicId, TopicResult::getTopicLabel));

        log.info("Budowanie DTO");
        List<TopicSentimentStats> stats = grouped.entrySet().stream()
                .map(e -> buildStats(modelId.intValue(),
                        e.getKey(),
                        topicLabels.get(e.getKey()),
                        e.getValue(),
                        request.sentimentModelStrategy(),
                        request.tokenStrategy()))
                .sorted(Comparator.comparingInt(TopicSentimentStats::getTopicId))
                .toList();

        log.info("Zapis do db");
        persist(stats);
        log.info("Zapis do jsona");
        exportJson(stats);

        return stats;
    }

    /** Mapuje processedTweet.id → topicId */
    private Map<Long, Integer> buildProcessedIdTopicMap(Long modelId) {

        List<DocumentTopicAssignment> assignments =
                documentTopicAssignmentRepository.findByTopicModelingResultId(modelId);

        if (assignments.isEmpty()) return Map.of();

        Map<Long, Integer> originalToTopic = new HashMap<>();
        assignments.forEach(a ->
                parseIds(a.getTweetIds())
                        .forEach(id -> originalToTopic.put(id, a.getDominantTopicId()))
        );

        if (originalToTopic.isEmpty()) return Map.of();

        return processedTweetRepository.findByOriginalTweetIdIn(originalToTopic.keySet()).stream()
                .collect(Collectors.toMap(
                        ProcessedTweet::getId,
                        pt -> originalToTopic.get(pt.getOriginalTweet().getId())
                ));
    }

    private List<Long> parseIds(@Nullable String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("Niepoprawny JSON z tweetIds: {}", e.getMessage());
            return List.of();
        }
    }

    private TopicSentimentStats buildStats(int modelId,
                                           int topicId,
                                           String topicLabel,
                                           List<SentimentResult> list,
                                           SentimentStrategyLabel sentimentStrategyLabel,
                                           TokenStrategyLabel tokenStrategyLabel) {

        long pos, neu, neg;
        double avgScore;

        if (sentimentStrategyLabel == SentimentStrategyLabel.STANDARD) {   // leksykon
            List<Double> scores = list.stream()
                    .map(SentimentResult::getScore)
                    .toList();
            pos = scores.stream().filter(s -> s >  0.1).count();
            neg = scores.stream().filter(s -> s < -0.1).count();
            neu = scores.size() - pos - neg;
            avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        } else {                                             // HF_API
            pos = list.stream().filter(r -> r.getLabel() == SentimentLabel.POSITIVE).count();
            neg = list.stream().filter(r -> r.getLabel() == SentimentLabel.NEGATIVE).count();
            neu = list.size() - pos - neg;
            avgScore = list.stream().mapToDouble(SentimentResult::getScore).average().orElse(0);
        }

        long total = list.size();

        return TopicSentimentStats.builder()
                .modelId(modelId)
                .tokenStrategy(tokenStrategyLabel)
                .sentimentModelStrategy(sentimentStrategyLabel)
                .topicId(topicId)
                .topicResultLabel(Optional.ofNullable(topicLabel).orElse("n/a"))
                .positive(pos)
                .neutral(neu)
                .negative(neg)
                .total(total)
                .positiveRatio(round(pos * 100.0 / total))
                .negativeRatio(round(neg * 100.0 / total))
                .avgScore(round(avgScore))
                .build();
    }

    private void persist(List<TopicSentimentStats> stats) {
        List<TopicSentimentStatsEntity> entities = stats.stream()
                .map(this::mapToEntity)
                .toList();
        topicSentimentStatsRepository.saveAll(entities);
    }

    private TopicSentimentStatsEntity mapToEntity(TopicSentimentStats dto) {
        return TopicSentimentStatsEntity.builder()
                .modelId(dto.getModelId())
                .tokenStrategy(dto.getTokenStrategy())
                .sentimentModelStrategy(dto.getSentimentModelStrategy())
                .topicId(dto.getTopicId())
                .topicResultLabel(dto.getTopicResultLabel())
                .positive(dto.getPositive())
                .neutral(dto.getNeutral())
                .negative(dto.getNegative())
                .totalTweets(dto.getTotal())
                .positiveRatio(dto.getPositiveRatio())
                .negativeRatio(dto.getNegativeRatio())
                .avgScore(dto.getAvgScore())
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    private void exportJson(List<TopicSentimentStats> stats) {
        if (stats.isEmpty()) return;

        TopicSentimentStats first = stats.get(0);
        String file = String.format(
                "output/sentiment_responses/%d_%s_%s_%tF.json",
                first.getModelId(),
                first.getTokenStrategy().name().toLowerCase(),
                first.getSentimentModelStrategy().name().toLowerCase(),
                LocalDate.now());

        try {
            Files.createDirectories(Path.of("output/sentiment_responses"));
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(Path.of(file).toFile(), stats);
            log.info("Sentiment stats exported to {}", file);
        } catch (Exception e) {
            log.error("Export failed: {}", e.getMessage());
        }
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
