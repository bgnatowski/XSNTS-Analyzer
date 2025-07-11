package pl.bgnat.master.xscrapper.service.normalization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.repository.normalization.ProcessedTweetRepository;
import pl.bgnat.master.xscrapper.repository.scrapper.TweetRepository;
import pl.bgnat.master.xscrapper.dto.normalization.ProcessingStatsDTO;

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
    public ProcessingStatsDTO calculateStats() {
        long totalTweets = tweetRepository.count();
        long normalizedTweets = processedTweetRepository.count();
        long averageTokens = calculateAverageTokens();
        double processingProgress = calculateProgress(normalizedTweets, totalTweets);

        return ProcessingStatsDTO.builder()
                .totalTweets(totalTweets)
                .processedTweets(normalizedTweets)
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
