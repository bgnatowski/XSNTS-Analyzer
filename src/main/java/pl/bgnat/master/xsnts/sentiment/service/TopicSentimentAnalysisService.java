package pl.bgnat.master.xsnts.sentiment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xsnts.sentiment.model.TopicSentimentStats;
import pl.bgnat.master.xsnts.topicmodeling.model.DocumentTopicAssignment;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicResult;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;
import pl.bgnat.master.xsnts.topicmodeling.repository.DocumentTopicAssignmentRepository;
import pl.bgnat.master.xsnts.topicmodeling.repository.TopicResultRepository;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.sentiment.repository.SentimentResultRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicSentimentAnalysisService {

    private final TopicResultRepository topicResultRepo;
    private final DocumentTopicAssignmentRepository docTopicRepo;
    private final ProcessedTweetRepository processedTweetRepo;
    private final SentimentResultRepository sentimentRepo;
    private final ObjectMapper objectMapper;

    /**
     * Zwraca statystyki sentymentu dla każdego tematu (topicId) w danym modelu.
     */
    public List<TopicSentimentStats> getSentimentStatsForModel(Long topicModelingResultId) {
        List<TopicResult> topics = topicResultRepo.findByTopicModelingResultIdOrderByTopicId(topicModelingResultId);
        List<DocumentTopicAssignment> assignments = docTopicRepo.findByTopicModelingResultId(topicModelingResultId);

        // Map: topicId -> lista wszystkich tweetId przypisanych do tego tematu
        Map<Integer, List<Long>> topicToTweetIds = new HashMap<>();
        for (DocumentTopicAssignment dta : assignments) {
            List<Long> tweetIds = parseTweetIds(dta.getTweetIds());
            topicToTweetIds.computeIfAbsent(dta.getDominantTopicId(), k -> new ArrayList<>()).addAll(tweetIds);
        }

        // Map: tweetId -> SentimentResult
        Set<Long> allTweetIds = topicToTweetIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, SentimentResult> tweetIdToSentiment = sentimentRepo.findAllByProcessedTweetIdIn(allTweetIds).stream()
                .collect(Collectors.toMap(
                        sr -> sr.getProcessedTweet().getOriginalTweet().getId(),
                        sr -> sr
                ));

        // Zbuduj statystyki dla każdego tematu
        List<TopicSentimentStats> result = new ArrayList<>();
        for (TopicResult topic : topics) {
            List<Long> tweetIds = topicToTweetIds.getOrDefault(topic.getTopicId(), Collections.emptyList());
            long pos = 0, neu = 0, neg = 0;
            double sumScore = 0;
            for (Long tweetId : tweetIds) {
                SentimentResult sr = tweetIdToSentiment.get(tweetId);
                if (sr == null) continue;
                switch (sr.getLabel()) {
                    case POSITIVE -> pos++;
                    case NEUTRAL -> neu++;
                    case NEGATIVE -> neg++;
                }
                sumScore += sr.getScore();
            }
            long total = pos + neu + neg;
            result.add(TopicSentimentStats.builder()
                    .topicId(topic.getTopicId())
                    .topicLabel(topic.getTopicLabel())
                    .positive(pos)
                    .neutral(neu)
                    .negative(neg)
                    .total(total)
                    .positiveRatio(total > 0 ? (double) pos / total : 0)
                    .negativeRatio(total > 0 ? (double) neg / total : 0)
                    .avgScore(total > 0 ? sumScore / total : 0)
                    .build());
        }
        return result;
    }

    private List<Long> parseTweetIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
