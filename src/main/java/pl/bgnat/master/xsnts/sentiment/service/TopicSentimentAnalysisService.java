package pl.bgnat.master.xsnts.sentiment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.TopicSentimentStats;
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

    private final DocumentTopicAssignmentRepository assignmentRepo;
    private final TopicResultRepository topicResultRepo;
    private final ProcessedTweetRepository processedTweetRepo;
    private final SentimentResultRepository sentimentRepo;
    private final TopicSentimentStatsRepository statsRepo;
    private final ObjectMapper mapper;

    /* ============================================================ */
    /* ======================   API PUBLICZNE   ==================== */
    /* ============================================================ */

    @Transactional
    public List<TopicSentimentStats> getSentimentStatsForModel(Long modelId) {

        /* -------- 1. zestawienie tweet → topic ------------------ */
        Map<Long, Integer> processedIdToTopic = buildProcessedIdTopicMap(modelId);
        if (processedIdToTopic.isEmpty()) {
            log.warn("Model {} nie posiada tweetów z przypisanym tematem", modelId);
            return List.of();
        }

        /* -------- 2. pobranie wyników sentymentu ---------------- */
        Map<Integer, List<Double>> groupedScores = sentimentRepo
                .findAllByProcessedTweetIdIn(processedIdToTopic.keySet()).stream()
                .filter(sr -> processedIdToTopic.containsKey(sr.getProcessedTweet().getId()))
                .collect(Collectors.groupingBy(
                        sr -> processedIdToTopic.get(sr.getProcessedTweet().getId()),
                        Collectors.mapping(SentimentResult::getScore, Collectors.toList())
                ));

        long orphan = sentimentRepo.countByProcessedTweetIdNotIn(processedIdToTopic.keySet());
        log.info("Model {} – wyników sentymentu bez dopasowania: {}", modelId, orphan);

        if (groupedScores.isEmpty()) {
            log.warn("Model {} – brak dopasowanych wyników sentymentu", modelId);
            return List.of();
        }

        /* -------- 3. etykiety tematów --------------------------- */
        Map<Integer, String> topicLabels = topicResultRepo
                .findByTopicModelingResultId(modelId).stream()
                .collect(Collectors.toMap(TopicResult::getTopicId,
                        TopicResult::getTopicLabel));

        /* -------- 4. budowa DTO -------------------------------- */
        List<TopicSentimentStats> stats = groupedScores.entrySet().stream()
                .map(e -> buildStats(modelId.intValue(),
                        e.getKey(),
                        topicLabels.get(e.getKey()),
                        e.getValue()))
                .sorted(Comparator.comparingInt(TopicSentimentStats::getTopicId))
                .toList();

        /* -------- 5. zapis + eksport --------------------------- */
        persist(stats);
        exportJson(stats);

        return stats;
    }


    /** Mapuje processedTweet.id → topicId dla wszystkich tweetów użytych w modelu */
    private Map<Long, Integer> buildProcessedIdTopicMap(Long modelId) {

        List<DocumentTopicAssignment> assignments =
                assignmentRepo.findByTopicModelingResultId(modelId);

        if (assignments.isEmpty()) return Map.of();

        /* originalTweetId → topicId */
        Map<Long, Integer> originalToTopic = new HashMap<>();
        assignments.forEach(a ->
                parseIds(a.getTweetIds())
                        .forEach(id -> originalToTopic.put(id, a.getDominantTopicId()))
        );

        if (originalToTopic.isEmpty()) return Map.of();

        /* processedTweet.id → topicId */
        return processedTweetRepo.findByOriginalTweetIdIn(originalToTopic.keySet()).stream()
                .collect(Collectors.toMap(
                        ProcessedTweet::getId,
                        pt -> originalToTopic.get(pt.getOriginalTweet().getId())
                ));
    }

    private List<Long> parseIds(@Nullable String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("Niepoprawny JSON z tweetIds: {}", e.getMessage());
            return List.of();
        }
    }

    private TopicSentimentStats buildStats(int modelId,
                                           int topicId,
                                           String topicLabel,
                                           List<Double> scores) {

        long positive = scores.stream().filter(s -> s > 0.1).count();
        long negative = scores.stream().filter(s -> s < -0.1).count();
        long total    = scores.size();
        long neutral  = total - positive - negative;
        double avg    = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        return TopicSentimentStats.builder()
                .modelId(modelId)
                .tokenStrategy(TokenStrategyLabel.NORMAL)
                .sentimentModelStrategy(SentimentStrategyLabel.STANDARD)
                .topicId(topicId)
                .topicResultLabel(Optional.ofNullable(topicLabel).orElse("n/a"))
                .positive(positive)
                .neutral(neutral)
                .negative(negative)
                .total(total)
                .positiveRatio(round(positive * 100.0 / total))
                .negativeRatio(round(negative * 100.0 / total))
                .avgScore(round(avg))
                .build();
    }

    private void persist(List<TopicSentimentStats> stats) {
        List<TopicSentimentStatsEntity> entities = stats.stream()
                .map(this::mapToEntity)
                .toList();
        statsRepo.saveAll(entities);
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
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(Path.of(file).toFile(), stats);

            log.info("Sentiment stats exported to {}", file);
        } catch (Exception e) {
            log.error("Export failed: {}", e.getMessage());
        }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
