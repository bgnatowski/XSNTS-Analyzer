package pl.bgnat.master.xscrapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xscrapper.model.ProcessedTweet;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.repository.ProcessedTweetRepository;
import pl.bgnat.master.xscrapper.repository.TweetRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TweetProcessingService {

    private final TweetRepository tweetRepository;
    private final ProcessedTweetRepository processedTweetRepository;
    private final TextNormalizer textNormalizer;
    private final ObjectMapper objectMapper;

    public ProcessedTweet processTweet(Tweet tweet) {
        // Sprawdź czy tweet już został przetworzony
        if (processedTweetRepository.existsByOriginalTweetId(tweet.getId())) {
            log.debug("Tweet {} już został przetworzony", tweet.getId());
            return processedTweetRepository.findByOriginalTweetId(tweet.getId()).orElse(null);
        }

        try {
            TextNormalizer.NormalizedTweet result = textNormalizer.processText(tweet.getContent());

            ProcessedTweet processedTweet = ProcessedTweet.builder()
                    .originalTweet(tweet)
                    .normalizedContent(result.getNormalizedContent())
                    .tokens(objectMapper.writeValueAsString(result.getTokens()))
                    .tokenCount(result.getTokenCount())
                    .processedDate(LocalDateTime.now())
                    .build();

            return processedTweetRepository.save(processedTweet);

        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania tweeta {}: {}", tweet.getId(), e.getMessage());
            return null;
        }
    }

    public void processAllTweets() {
        log.info("Rozpoczynam przetwarzanie wszystkich tweetów");

        int pageSize = 100;
        int pageNumber = 0;
        Page<Tweet> page;
        int processedCount = 0;

        do {
            page = tweetRepository.findAll(PageRequest.of(pageNumber, pageSize));

            for (Tweet tweet : page.getContent()) {
                ProcessedTweet processed = processTweet(tweet);
                if (processed != null) {
                    processedCount++;
                }
            }

            pageNumber++;
            log.info("Przetworzono {} z {} tweetów", processedCount, tweetRepository.count());

        } while (page.hasNext());

        log.info("Zakończono przetwarzanie. Przetworzono {} tweetów", processedCount);
    }

    public String exportToCsv(String filePath) throws IOException {
        log.info("Eksport do CSV: {}", filePath);

        List<ProcessedTweet> processedTweets = processedTweetRepository.findAll();

        try (FileWriter writer = new FileWriter(filePath)) {
            // Nagłówek CSV
            writer.append("tweet_id,username,original_content,normalized_content,tokens,token_count,post_date\n");

            for (ProcessedTweet processed : processedTweets) {
                Tweet originalTweet = processed.getOriginalTweet();

                writer.append(String.valueOf(originalTweet.getId())).append(",");
                writer.append("\"").append(escapeForCsv(originalTweet.getUsername())).append("\",");
                writer.append("\"").append(escapeForCsv(originalTweet.getContent())).append("\",");
                writer.append("\"").append(escapeForCsv(processed.getNormalizedContent())).append("\",");
                writer.append("\"").append(escapeForCsv(processed.getTokens())).append("\",");
                writer.append(String.valueOf(processed.getTokenCount())).append(",");
                writer.append("\"").append(originalTweet.getPostDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\"");
                writer.append("\n");
            }
        }

        log.info("Eksport zakończony. Wyeksportowano {} rekordów", processedTweets.size());
        return filePath;
    }

    private String escapeForCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    public ProcessingStats getProcessingStats() {
        long totalTweets = tweetRepository.count();
        long processedTweets = processedTweetRepository.count();
        long averageTokens = processedTweetRepository.findAll()
                .stream()
                .mapToLong(ProcessedTweet::getTokenCount)
                .sum() / Math.max(processedTweets, 1);

        return ProcessingStats.builder()
                .totalTweets(totalTweets)
                .processedTweets(processedTweets)
                .averageTokensPerTweet(averageTokens)
                .processingProgress((double) processedTweets / totalTweets * 100)
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class ProcessingStats {
        private Long totalTweets;
        private Long processedTweets;
        private Long averageTokensPerTweet;
        private Double processingProgress;
    }
    /**
     * Usuwa rekordy z tabeli processed_tweet gdzie normalized_content i tokens są puste
     * @return liczba usuniętych rekordów
     */
    public CleanupResult cleanupEmptyRecords() {
        log.info("Rozpoczynam usuwanie pustych rekordów z processed_tweet");

        try {
            // Najpierw sprawdź ile rekordów będzie usuniętych
            long emptyRecordsCount = processedTweetRepository.countEmptyRecords();
            log.info("Znaleziono {} pustych rekordów do usunięcia", emptyRecordsCount);

            if (emptyRecordsCount == 0) {
                return CleanupResult.builder()
                        .deletedRecords(0)
                        .success(true)
                        .message("Brak pustych rekordów do usunięcia")
                        .build();
            }

            // Usuń puste rekordy
            int deletedCount = processedTweetRepository.deleteEmptyRecords();

            log.info("Usunięto {} pustych rekordów", deletedCount);

            return CleanupResult.builder()
                    .deletedRecords(deletedCount)
                    .success(true)
                    .message("Pomyślnie usunięto " + deletedCount + " pustych rekordów")
                    .build();

        } catch (Exception e) {
            log.error("Błąd podczas usuwania pustych rekordów: {}", e.getMessage());
            return CleanupResult.builder()
                    .deletedRecords(0)
                    .success(false)
                    .message("Błąd podczas usuwania: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Zwraca listę pustych rekordów (do podglądu przed usunięciem)
     * @return lista pustych rekordów
     */
    public List<ProcessedTweet> getEmptyRecords() {
        return processedTweetRepository.findEmptyRecords();
    }

    /**
     * Zwraca liczbę pustych rekordów
     * @return liczba pustych rekordów
     */
    public long getEmptyRecordsCount() {
        return processedTweetRepository.countEmptyRecords();
    }

    @lombok.Builder
    @lombok.Data
    public static class CleanupResult {
        private Integer deletedRecords;
        private Boolean success;
        private String message;
    }
}
