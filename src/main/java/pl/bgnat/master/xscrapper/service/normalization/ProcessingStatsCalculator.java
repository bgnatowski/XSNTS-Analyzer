package pl.bgnat.master.xscrapper.service.normalization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.repository.ProcessedTweetRepository;
import pl.bgnat.master.xscrapper.repository.TweetRepository;
import pl.bgnat.master.xscrapper.dto.ProcessingStats;

/**
 * Komponent odpowiedzialny za kalkulację statystyk przetwarzania
 */
@Component
@RequiredArgsConstructor
public class ProcessingStatsCalculator {

    private final TweetRepository tweetRepository;
    private final ProcessedTweetRepository processedTweetRepository;

    /**
     * Oblicza aktualne statystyki przetwarzania
     * @return obiekt ProcessingStats
     */
    public ProcessingStats calculateStats() {
        long totalTweets = tweetRepository.count();
        long processedTweets = processedTweetRepository.count();
        long averageTokens = calculateAverageTokens();
        double processingProgress = calculateProgress(processedTweets, totalTweets);

        return ProcessingStats.builder()
                .totalTweets(totalTweets)
                .processedTweets(processedTweets)
                .averageTokensPerTweet(averageTokens)
                .processingProgress(processingProgress)
                .build();
    }

    private long calculateAverageTokens() {
        Long averageTokens = processedTweetRepository.getAverageTokenCount();
        return averageTokens != null ? averageTokens : 0L;
    }

    private double calculateProgress(long processed, long total) {
        return total > 0 ? (double) processed / total * 100 : 0.0;
    }
}
