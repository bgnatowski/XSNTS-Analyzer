package pl.bgnat.master.xsnts.normalization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.normalization.service.cleaning.EmptyRecordsCleaner;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.scrapper.repository.TweetRepository;
import pl.bgnat.master.xsnts.normalization.dto.CleanupResult;
import pl.bgnat.master.xsnts.normalization.dto.ProcessingStatsDTO;
import pl.bgnat.master.xsnts.normalization.service.processing.ProcessingStatsCalculator;
import pl.bgnat.master.xsnts.normalization.service.processing.PolishTweetProcessor;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Serwis odpowiedzialny za przetwarzanie tweetów
 * Wykonuje normalizację, tokenizację i zarządzanie przetworzonymi danymi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TweetProcessingService {

    public static final int MIN_TOKEN_COUNT = 3;
    public static final Predicate<ProcessedTweet> PROCESSED_TWEET_PREDICATE = processedTweet -> processedTweet.getTokenCount() > MIN_TOKEN_COUNT;

    private final TweetRepository tweetRepository;
    private final ProcessedTweetRepository processedTweetRepository;

    private final PolishTweetProcessor tweetProcessor;
    private final ProcessingStatsCalculator statsCalculator;

    private final EmptyRecordsCleaner emptyRecordsCleaner;

    @Value("${app.processing.batch-size:500}")
    private int batchSize;

    @Value("${app.processing.progress-interval:1000}")
    private int progressInterval;

    @Transactional
    public String processAllTweets() {
        log.info("Rozpoczynam zoptymalizowane przetwarzanie wszystkich tweetów");

        long startTime = System.currentTimeMillis();
        AtomicInteger totalProcessed = new AtomicInteger(0);

        // Pobierz tylko ID tweetów, które zostały przetworzone
        Set<Long> processedTweetIds = getProcessedTweetIds();
        long totalTweets = getTotalTweetCount();

        log.info("Znaleziono {} tweetów do przetworzenia (z {} całkowitych)", totalTweets - processedTweetIds.size(), totalTweets);

        processUnprocessedTweetsInBatches(processedTweetIds, totalProcessed, totalTweets);

        long endTime = System.currentTimeMillis();
        String processedResult = "Zakończono przetwarzanie. Przetworzono %s nowych tweetów w %d ms".formatted(totalTweets, endTime - startTime);
        log.info(processedResult);
        return processedResult;
    }

    /**
     * Pobiera statystyki przetwarzania
     *
     * @return obiekt ProcessingStats z aktualnymi statystykami
     */
    public ProcessingStatsDTO getProcessingStats() {
        return statsCalculator.calculateStats();
    }

    public CleanupResult cleanupEmptyRecords() {
        return emptyRecordsCleaner.cleanupEmptyRecords();
    }

    public List<ProcessedTweet> getEmptyRecords() {
        return emptyRecordsCleaner.getEmptyRecords();
    }

    public long getEmptyRecordsCount() {
        return emptyRecordsCleaner.getEmptyRecordsCount();
    }


    private void processUnprocessedTweetsInBatches(Set<Long> processedTweetIds, AtomicInteger totalProcessed, long totalTweets) {
        int pageNumber = 0;
        Page<Tweet> page;

        log.info("Zaczynam przetwarzanie w batchach");
        do {
            page = tweetRepository.findAll(PageRequest.of(pageNumber, batchSize));
//
            List<Tweet> unprocessedTweets = page.getContent().stream()
                    .filter(tweet -> !processedTweetIds.contains(tweet.getId()))
                    .collect(Collectors.toList());
//            List<Tweet> unprocessedTweets = tweetRepository.findById(6L).stream().toList();  // test pojecyczy
            if (!unprocessedTweets.isEmpty()) {
                log.info("Rozpoczynam batch: {}. Przetworzono {}/{}", pageNumber, totalProcessed.get(), totalTweets);
                int batchProcessed = processTweetsBatch(unprocessedTweets);
                totalProcessed.addAndGet(batchProcessed);

                logProcessingProgress(totalProcessed.get());
            }

            pageNumber++;

        } while (page.hasNext());
    }

    private int processTweetsBatch(List<Tweet> tweets) {
        List<ProcessedTweet> processedTweets = tweets.stream()
                .map(tweetProcessor::processTweet)
                .filter(Objects::nonNull)
                .filter(PROCESSED_TWEET_PREDICATE)
                .collect(Collectors.toList());

        if (!processedTweets.isEmpty()) {
            processedTweetRepository.saveAll(processedTweets);
        }

        return processedTweets.size();
    }

    private Set<Long> getProcessedTweetIds() {
        return processedTweetRepository.findAllProcessedTweetIds();
    }

    private long getTotalTweetCount() {
        return tweetRepository.count();
    }

    private void logProcessingProgress(int processed) {
        if (processed % progressInterval == 0) {
            log.info("Przetworzono {} tweetów", processed);
        }
    }
}
