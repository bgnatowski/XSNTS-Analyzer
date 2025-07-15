package pl.bgnat.master.xscrapper.service.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import pl.bgnat.master.xscrapper.model.normalization.ProcessedTweet;
import pl.bgnat.master.xscrapper.model.scrapper.Tweet;
import pl.bgnat.master.xscrapper.model.sentiment.SentimentResult;
import pl.bgnat.master.xscrapper.model.topicmodeling.DocumentTopicAssignment;
import pl.bgnat.master.xscrapper.model.topicmodeling.TopicResult;
import pl.bgnat.master.xscrapper.repository.normalization.ProcessedTweetRepository;
import pl.bgnat.master.xscrapper.repository.sentiment.SentimentResultRepository;
import pl.bgnat.master.xscrapper.repository.topicmodeling.DocumentTopicAssignmentRepository;
import pl.bgnat.master.xscrapper.repository.topicmodeling.TopicResultRepository;
import pl.bgnat.master.xscrapper.utils.CsvWriterUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] HEAD_PROCESSED = { "tweet_id","username","original_content",
            "normalized_content","tokens","token_count","post_date" };
    private static final String[] HEAD_SENTIMENT = {
            "tweet_id", "username", "normalized_content",
            "token_count", "sentiment_label", "sentiment_score",
            "likes", "reposts", "comments", "views", "post_date"
    };
    private static final String[] HEAD_TOPIC_SENTIMENT = {
            "topic_id", "topic_label", "tweet_id", "username", "original_content",
            "normalized_content", "token_count", "sentiment_label", "sentiment_score",
            "likes", "reposts", "comments", "views", "post_date"
    };

    private final ProcessedTweetRepository processedRepo;
    private final SentimentResultRepository sentimentRepo;
    private final TopicResultRepository topicResultRepo;
    private final DocumentTopicAssignmentRepository docTopicRepo;
    private final ObjectMapper objectMapper;

    // --- dotychczasowe metody eksportu ---

    public String exportProcessedTweets(String userPath) throws IOException {
        String path = CsvWriterUtil.defaultName("processed", userPath);
        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_PROCESSED);
            for (ProcessedTweet p : processedRepo.findAll()) writeProcessed(w, p);
        }
        log.info("Eksport processed: {}", path);
        return path;
    }

    public String exportSentimentResults(String userPath) throws IOException {
        String path = CsvWriterUtil.defaultName("sentiment", userPath);
        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_SENTIMENT);
            for (SentimentResult r : sentimentRepo.findAllWithDependencies()) writeSentiment(w, r);
        }
        log.info("Eksport sentiment: {}", path);
        return path;
    }

    public String exportTopicSentimentTweets(Long topicModelingResultId, String userPath) throws IOException {
        String path = CsvWriterUtil.defaultName("topic_sentiment", userPath);

        List<TopicResult> topics = topicResultRepo.findByTopicModelingResultIdOrderByTopicId(topicModelingResultId);
        Map<Integer, String> topicLabels = new HashMap<>();
        for (TopicResult t : topics) topicLabels.put(t.getTopicId(), t.getTopicLabel());

        List<DocumentTopicAssignment> assignments = docTopicRepo.findByTopicModelingResultId(topicModelingResultId);
        Map<Integer, List<Long>> topicToTweetIds = new HashMap<>();
        for (DocumentTopicAssignment dta : assignments) {
            List<Long> tweetIds = parseTweetIds(dta.getTweetIds());
            topicToTweetIds.computeIfAbsent(dta.getDominantTopicId(), k -> new ArrayList<>()).addAll(tweetIds);
        }

        Set<Long> allTweetIds = new HashSet<>();
        topicToTweetIds.values().forEach(allTweetIds::addAll);

        Map<Long, ProcessedTweet> tweetMap = new HashMap<>();
        for (ProcessedTweet pt : processedRepo.findAllByOriginalTweetIdIn(allTweetIds)) {
            tweetMap.put(pt.getOriginalTweet().getId(), pt);
        }

        Map<Long, SentimentResult> sentimentMap = new HashMap<>();
        for (SentimentResult sr : sentimentRepo.findAllByProcessedTweetIdIn(allTweetIds)) {
            sentimentMap.put(sr.getProcessedTweet().getOriginalTweet().getId(), sr);
        }

        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_TOPIC_SENTIMENT);

            for (Map.Entry<Integer, List<Long>> entry : topicToTweetIds.entrySet()) {
                int topicId = entry.getKey();
                String label = topicLabels.getOrDefault(topicId, "");
                for (Long tweetId : entry.getValue()) {
                    ProcessedTweet p = tweetMap.get(tweetId);
                    SentimentResult s = sentimentMap.get(tweetId);
                    if (p == null || s == null) continue;
                    Tweet t = p.getOriginalTweet();
                    CsvWriterUtil.writeLine(w,
                            String.valueOf(topicId),
                            CsvWriterUtil.esc(label),
                            String.valueOf(tweetId),
                            CsvWriterUtil.esc(t.getUsername()),
                            CsvWriterUtil.esc(t.getContent()),
                            CsvWriterUtil.esc(p.getNormalizedContent()),
                            String.valueOf(p.getTokenCount()),
                            s.getLabel().name(),
                            String.valueOf(s.getScore()),
                            String.valueOf(t.getLikeCount()),
                            String.valueOf(t.getRepostCount()),
                            String.valueOf(t.getCommentCount()),
                            String.valueOf(t.getViews()),
                            CsvWriterUtil.esc(t.getPostDate().format(ISO))
                    );
                }
            }
        }
        log.info("Eksport topic_sentiment: {}", path);
        return path;
    }

    // --- metody pomocnicze ---

    private void writeProcessed(FileWriter w, ProcessedTweet p) throws IOException {
        Tweet t = p.getOriginalTweet();
        CsvWriterUtil.writeLine(w,
                String.valueOf(t.getId()),
                CsvWriterUtil.esc(t.getUsername()),
                CsvWriterUtil.esc(t.getContent()),
                CsvWriterUtil.esc(p.getNormalizedContent()),
                CsvWriterUtil.esc(p.getTokens()),
                String.valueOf(p.getTokenCount()),
                CsvWriterUtil.esc(t.getPostDate().format(ISO))
        );
    }

    private void writeSentiment(FileWriter w, SentimentResult r) throws IOException {
        ProcessedTweet p = r.getProcessedTweet();
        Tweet t = p.getOriginalTweet();
        CsvWriterUtil.writeLine(w,
                String.valueOf(t.getId()),
                CsvWriterUtil.esc(t.getUsername()),
                CsvWriterUtil.esc(p.getNormalizedContent()),
                String.valueOf(p.getTokenCount()),
                r.getLabel().name(),
                String.valueOf(r.getScore()),
                String.valueOf(t.getLikeCount()),
                String.valueOf(t.getRepostCount()),
                String.valueOf(t.getCommentCount()),
                String.valueOf(t.getViews()),
                CsvWriterUtil.esc(t.getPostDate().format(ISO))
        );
    }

    private List<Long> parseTweetIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
