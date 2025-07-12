package pl.bgnat.master.xsnts.normalization.service.cleaning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xsnts.normalization.dto.CleanupResult;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.normalization.service.LanguageDetectionService;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NonPolishTweetsCleaner {

    private final ProcessedTweetRepository processedTweetRepository;
    private final LanguageDetectionService languageDetectionService;

    private static final int BATCH_SIZE = 1000;

    @Transactional
    public CleanupResult cleanupNonPolishTweets() {
        log.info("Rozpoczynam usuwanie tweetów niepolskich z processed_tweet");

        try {
            List<Long> nonPolishTweetIds = findNonPolishTweetIds();

            if (nonPolishTweetIds.isEmpty()) {
                return CleanupResult.builder()
                        .deletedRecords(0)
                        .success(true)
                        .message("Nie znaleziono tweetów niepolskich do usunięcia")
                        .build();
            }

            log.info("Znaleziono {} tweetów niepolskich do usunięcia", nonPolishTweetIds.size());

            int deletedCount = deleteNonPolishTweetsInBatches(nonPolishTweetIds);

            log.info("Usunięto {} tweetów niepolskich", deletedCount);

            return CleanupResult.builder()
                    .deletedRecords(deletedCount)
                    .success(true)
                    .message("Pomyślnie usunięto " + deletedCount + " tweetów niepolskich")
                    .build();

        } catch (Exception e) {
            log.error("Błąd podczas usuwania tweetów niepolskich: {}", e.getMessage());
            return CleanupResult.builder()
                    .deletedRecords(0)
                    .success(false)
                    .message("Błąd podczas usuwania: " + e.getMessage())
                    .build();
        }
    }

    public long countNonPolishTweets() {
        log.info("Rozpoczynam liczenie tweetów niepolskich");

        long totalCount = processedTweetRepository.count();
        long nonPolishCount = 0;
        int pageNumber = 0;

        while (true) {
            Page<ProcessedTweet> page = processedTweetRepository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));

            if (page.isEmpty()) {
                break;
            }

            for (ProcessedTweet tweet : page.getContent()) {
                if (!isPolishTweet(tweet)) {
                    nonPolishCount++;
                }
            }

            pageNumber++;

            if (pageNumber % 10 == 0) {
                log.info("Przeanalizowano {} z {} tweetów", pageNumber * BATCH_SIZE, totalCount);
            }

            if (!page.hasNext()) {
                break;
            }
        }

        log.info("Znaleziono {} tweetów niepolskich z {} całkowitych", nonPolishCount, totalCount);
        return nonPolishCount;
    }

    private List<Long> findNonPolishTweetIds() {
        List<Long> nonPolishIds = new ArrayList<>();
        int pageNumber = 0;

        while (true) {
            Page<ProcessedTweet> page = processedTweetRepository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));

            if (page.isEmpty()) {
                break;
            }

            for (ProcessedTweet tweet : page.getContent()) {
                if (!isPolishTweet(tweet)) {
                    nonPolishIds.add(tweet.getId());
                }
            }

            pageNumber++;

            if (pageNumber % 10 == 0) {
                log.info("Przeanalizowano {} batchy, znaleziono {} niepolskich tweetów", pageNumber, nonPolishIds.size());
            }

            if (!page.hasNext()) {
                break;
            }
        }

        return nonPolishIds;
    }

    private boolean isPolishTweet(ProcessedTweet tweet) {
        if (hasLength(tweet.getNormalizedContent())) {
            return false;
        }
        String content = tweet.getNormalizedContent();
        return languageDetectionService.isPolish(content);
    }

    private int deleteNonPolishTweetsInBatches(List<Long> nonPolishTweetIds) {
        int totalDeleted = 0;

        for (int i = 0; i < nonPolishTweetIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, nonPolishTweetIds.size());
            List<Long> batch = nonPolishTweetIds.subList(i, endIndex);

            int deletedInBatch = processedTweetRepository.deleteByIds(batch);
            totalDeleted += deletedInBatch;

            log.info("Usunięto batch {}: {} rekordów", (i / BATCH_SIZE) + 1, deletedInBatch);
        }

        return totalDeleted;
    }
}
