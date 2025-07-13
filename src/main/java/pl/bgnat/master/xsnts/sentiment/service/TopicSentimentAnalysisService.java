package pl.bgnat.master.xsnts.sentiment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.TopicSentimentStats;
import pl.bgnat.master.xsnts.sentiment.model.TopicSentimentStatsEntity;
import pl.bgnat.master.xsnts.sentiment.repository.SentimentResultRepository;
import pl.bgnat.master.xsnts.sentiment.repository.TopicSentimentStatsRepository;
import pl.bgnat.master.xsnts.topicmodeling.model.DocumentTopicAssignment;
import pl.bgnat.master.xsnts.topicmodeling.repository.DocumentTopicAssignmentRepository;

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
    private final SentimentResultRepository sentimentRepo;
    private final TopicSentimentStatsRepository statsRepo;
    private final ObjectMapper mapper;

    @Transactional
    public List<TopicSentimentStats> getSentimentStatsForModel(Long modelId) {
        List<DocumentTopicAssignment> assignments = assignmentRepo.findByTopicModelingResultId(modelId);

        Map<Long, Integer> tweetToTopic = new HashMap<>();
        Map<Integer, String> topicLabels = new HashMap<>();

        // tweetIds są przechowywane w kolumnie JSON – zamieniamy na listę Long
        for (DocumentTopicAssignment a : assignments) {
            topicLabels.put(a.getDominantTopicId(), a.getDocumentId());
            List<Long> ids = parseIds(a.getTweetIds());
            ids.forEach(id -> tweetToTopic.put(id, a.getDominantTopicId()));
        }

        // Pobrane wyniki sentymentu pogrupowane po topicId
        Map<Integer, List<Double>> groupedScores =
                sentimentRepo.findAllByProcessedTweetIdIn(tweetToTopic.keySet()).stream()
                        .collect(Collectors.groupingBy(
                                r -> tweetToTopic.get(r.getProcessedTweet().getId()),
                                Collectors.mapping(r -> {
                                    // przechowujemy także label → agregacje będą potrzebne
                                    // tutaj zwracamy tylko score – label policzymy później
                                    return r.getScore();
                                }, Collectors.toList())
                        ));

        List<TopicSentimentStats> dtoList = groupedScores.entrySet().stream()
                .map(e -> buildStats(modelId.intValue(),
                        e.getKey(),
                        topicLabels.getOrDefault(e.getKey(), "N/A"),
                        e.getValue()))
                .sorted(Comparator.comparingInt(TopicSentimentStats::getTopicId))
                .toList();

        persist(dtoList);
        exportJson(dtoList);

        return dtoList;
    }

    /* ---------- helpers ---------- */

    private List<Long> parseIds(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Cannot parse tweetIds JSON", e);
            return List.of();
        }
    }

    private TopicSentimentStats buildStats(int modelId,
                                           int topicId,
                                           String topicLabel,
                                           List<Double> scores) {

        long pos = scores.stream().filter(s -> s > 0.1).count();
        long neg = scores.stream().filter(s -> s < -0.1).count();
        long tot = scores.size();
        long neu = tot - pos - neg;
        double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        return TopicSentimentStats.builder()
                .modelId(modelId)
                .tokenStrategy(TokenStrategyLabel.NORMAL)           // tylko NORMAL w tej iteracji
                .sentimentModelStrategy(SentimentStrategyLabel.STANDARD)
                .topicId(topicId)
                .topicLabel(topicLabel)
                .positive(pos)
                .neutral(neu)
                .negative(neg)
                .total(tot)
                .positiveRatio(round(pos * 100.0 / tot))
                .negativeRatio(round(neg * 100.0 / tot))
                .avgScore(round(avg))
                .build();
    }

    private void persist(List<TopicSentimentStats> stats) {
        List<TopicSentimentStatsEntity> entities = stats.stream()
                .map(this::toEntity)
                .toList();

        statsRepo.saveAll(entities);
    }

    private TopicSentimentStatsEntity toEntity(TopicSentimentStats dto) {
        return TopicSentimentStatsEntity.builder()
                .modelId(dto.getModelId())
                .tokenStrategy(dto.getTokenStrategy())
                .sentimentModelStrategy(dto.getSentimentModelStrategy())
                .topicId(dto.getTopicId())
                .topicLabel(dto.getTopicLabel())
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
            log.error("Export failed", e);
        }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
