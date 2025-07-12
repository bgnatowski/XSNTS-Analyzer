package pl.bgnat.master.xsnts.sentiment.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicSentimentStats {
    private int topicId;
    private String topicLabel;
    private long positive;
    private long neutral;
    private long negative;
    private long total;
    private double positiveRatio;
    private double negativeRatio;
    private double avgScore;
}
