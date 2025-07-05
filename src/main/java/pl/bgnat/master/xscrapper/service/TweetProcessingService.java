package pl.bgnat.master.xscrapper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xscrapper.model.ProcessedTweet;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.repository.ProcessedTweetRepository;
import pl.bgnat.master.xscrapper.repository.TweetRepository;
import pl.bgnat.master.xscrapper.dto.CleanupResult;
import pl.bgnat.master.xscrapper.dto.ProcessingStats;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Serwis odpowiedzialny za przetwarzanie tweetów
 * Wykonuje normalizację, tokenizację i zarządzanie przetworzonymi danymi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TweetProcessingService {

    // ========================================
    // KONFIGURACJA I ZALEŻNOŚCI
    // ========================================

    private static final int DEFAULT_BATCH_SIZE = 500;
    private static final int PROGRESS_LOG_INTERVAL = 1000;

    private final TweetRepository tweetRepository;
    private final ProcessedTweetRepository processedTweetRepository;
    private final TweetProcessor tweetProcessor;
    private final CsvExportService csvExportService;
    private final ProcessingStatsCalculator statsCalculator;
    private final EmptyRecordsCleaner emptyRecordsCleaner;

    @Value("${app.processing.batch-size:500}")
    private int batchSize;

    @Value("${app.processing.progress-interval:1000}")
    private int progressInterval;

    // ========================================
    // GŁÓWNE METODY PRZETWARZANIA
    // ========================================

    /**
     * Przetwarza pojedynczy tweet
     * @param tweet tweet do przetworzenia
     * @return przetworzony tweet lub null w przypadku błędu
     */
    @Transactional
    public ProcessedTweet processTweet(Tweet tweet) {
        validateTweet(tweet);

        if (isAlreadyProcessed(tweet.getId())) {
            log.debug("Tweet {} już został przetworzony", tweet.getId());
            return findProcessedTweet(tweet.getId());
        }

        return tweetProcessor.processAndSave(tweet);
    }

    /**
     * ZOPTYMALIZOWANA metoda przetwarzania wszystkich tweetów
     * Używa batch processing dla zwiększenia wydajności
     */
    @Transactional
    public void processAllTweets() {
        log.info("Rozpoczynam zoptymalizowane przetwarzanie wszystkich tweetów");

        long startTime = System.currentTimeMillis();
        AtomicInteger totalProcessed = new AtomicInteger(0);

        // Pobierz tylko ID tweetów które nie zostały jeszcze przetworzone
        Set<Long> processedTweetIds = getProcessedTweetIds();
        long totalTweets = getTotalTweetCount();

        log.info("Znaleziono {} tweetów do przetworzenia (z {} całkowitych)",
                totalTweets - processedTweetIds.size(), totalTweets);

        processUnprocessedTweetsBatch(processedTweetIds, totalProcessed);

        long endTime = System.currentTimeMillis();
        log.info("Zakończono przetwarzanie. Przetworzono {} nowych tweetów w {} ms",
                totalProcessed.get(), endTime - startTime);
    }

    // ========================================
    // STATYSTYKI I EKSPORT
    // ========================================

    /**
     * Pobiera statystyki przetwarzania
     * @return obiekt ProcessingStats z aktualnymi statystykami
     */
    public ProcessingStats getProcessingStats() {
        return statsCalculator.calculateStats();
    }

    /**
     * Eksportuje przetworzone tweety do pliku CSV
     * @param filename nazwa pliku wyjściowego
     * @return ścieżka do utworzonego pliku
     * @throws IOException w przypadku błędu zapisu
     */
    public String exportToCsv(String filename) throws IOException {
        validateFilename(filename);
        return csvExportService.exportProcessedTweets(filename);
    }

    // ========================================
    // ZARZĄDZANIE PUSTYMI REKORDAMI
    // ========================================

    /**
     * Usuwa rekordy z pustymi polami
     * @return wynik operacji czyszczenia
     */
    public CleanupResult cleanupEmptyRecords() {
        return emptyRecordsCleaner.cleanupEmptyRecords();
    }

    /**
     * Zwraca listę pustych rekordów
     * @return lista pustych rekordów
     */
    public List<ProcessedTweet> getEmptyRecords() {
        return emptyRecordsCleaner.getEmptyRecords();
    }

    /**
     * Zwraca liczbę pustych rekordów
     * @return liczba pustych rekordów
     */
    public long getEmptyRecordsCount() {
        return emptyRecordsCleaner.getEmptyRecordsCount();
    }

    // ========================================
    // METODY POMOCNICZE
    // ========================================

    private void processUnprocessedTweetsBatch(Set<Long> processedTweetIds, AtomicInteger totalProcessed) {
        int pageNumber = 0;
        Page<Tweet> page;

        do {
            page = tweetRepository.findAll(PageRequest.of(pageNumber, batchSize));

            List<Tweet> unprocessedTweets = page.getContent().stream()
                    .filter(tweet -> !processedTweetIds.contains(tweet.getId()))
                    .collect(Collectors.toList());

            if (!unprocessedTweets.isEmpty()) {
                int batchProcessed = processTweetsBatch(unprocessedTweets);
                totalProcessed.addAndGet(batchProcessed);

                logProgressIfNeeded(totalProcessed.get());
            }

            pageNumber++;

        } while (page.hasNext());
    }

    private int processTweetsBatch(List<Tweet> tweets) {
        List<ProcessedTweet> processedTweets = tweets.stream()
                .map(tweetProcessor::processWithoutSave)
                .filter(processedTweet -> processedTweet != null)
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

    private void logProgressIfNeeded(int processed) {
        if (processed % progressInterval == 0) {
            log.info("Przetworzono {} tweetów", processed);
        }
    }

    private boolean isAlreadyProcessed(Long tweetId) {
        return processedTweetRepository.existsByOriginalTweetId(tweetId);
    }

    private ProcessedTweet findProcessedTweet(Long tweetId) {
        return processedTweetRepository.findByOriginalTweetId(tweetId).orElse(null);
    }

    private void validateTweet(Tweet tweet) {
        if (tweet == null) {
            throw new IllegalArgumentException("Tweet nie może być null");
        }
        if (tweet.getId() == null) {
            throw new IllegalArgumentException("Tweet ID nie może być null");
        }
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Nazwa pliku nie może być pusta");
        }
    }
}
