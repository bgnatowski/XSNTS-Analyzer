package pl.bgnat.master.xscrapper.dto.normalization;

import lombok.Builder;
import lombok.Data;

/**
 * DTO zawierające statystyki przetwarzania tweetów
 */
@Data
@Builder
public class ProcessingStatsDTO {
    private Long totalTweets;
    private Long processedTweets;
    private Long averageTokensPerTweet;
    private Double processingProgress; // % ilość total / normalizzed
}
