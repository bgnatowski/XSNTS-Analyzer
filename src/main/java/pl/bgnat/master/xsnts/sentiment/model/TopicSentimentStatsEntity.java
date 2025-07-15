package pl.bgnat.master.xsnts.sentiment.model;

import jakarta.persistence.*;
import lombok.*;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_sentiment_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "model_id", "topic_id", "token_strategy", "sentiment_model_strategy"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicSentimentStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "topic_sentiment_stats_seq")
    @SequenceGenerator(name = "topic_sentiment_stats_seq",
            sequenceName = "topic_sentiment_stats_seq",
            allocationSize = 1)
    private Long id;

    @Column(name = "model_id", nullable = false)
    private int modelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_strategy", nullable = false)
    private TokenStrategyLabel tokenStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_model_strategy", nullable = false)
    private SentimentStrategyLabel sentimentModelStrategy;

    @Column(name = "topic_id", nullable = false)
    private int topicId;             // z document_topic_assginment

    @Column(name = "topic_result_label", length = 128, nullable = false)
    private String topicResultLabel; // 3 top wordsy z topic_result

    private long positive;
    private long neutral;
    private long negative;
    private long totalTweets;
    private double positiveRatio;
    private double negativeRatio;
    private double avgScore;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}
