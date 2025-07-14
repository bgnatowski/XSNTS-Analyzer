package pl.bgnat.master.xsnts.sentiment.dto;

import lombok.Builder;
import lombok.Data;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;

@Data
@Builder
public class TopicSentimentStats {
    private int modelId;            // id modelu
    private TokenStrategyLabel tokenStrategy;
    private SentimentStrategyLabel sentimentModelStrategy;
    private int topicId;                    // id topicu (3 topwordsy)
    private String topicResultLabel;      // id document topicu
    private long positive;          // ilość tweetów pozytywnych w temacie
    private long neutral;           // ilość tweetów neutralnych w temacie
    private long negative;          // ilość tweetów negatywnych w temacie
    private long total;             // ilość tweetów w temacie
    private double positiveRatio;   // % pozytynosci
    private double negativeRatio;   // % negatywnosci
    private double avgScore;
}
