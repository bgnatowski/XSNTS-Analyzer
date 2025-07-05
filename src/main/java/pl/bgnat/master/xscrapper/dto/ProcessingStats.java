package pl.bgnat.master.xscrapper.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO zawierające statystyki przetwarzania tweetów
 */
@Data
@Builder
public class ProcessingStats {
    private Long totalTweets;
    private Long normalizedTweets;
    private Long averageTokensPerTweet;
    private Double processingProgress;
}
