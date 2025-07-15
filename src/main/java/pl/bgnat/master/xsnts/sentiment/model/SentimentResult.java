package pl.bgnat.master.xsnts.sentiment.model;

import jakarta.persistence.*;
import lombok.*;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;

import java.time.LocalDateTime;

@Entity
@Table(name = "sentiment_result",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "processed_tweet_id",
                "token_strategy",
                "sentiment_model_strategy"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SentimentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "sentiment_result_seq")
    @SequenceGenerator(name = "sentiment_result_seq",
            sequenceName = "sentiment_result_seq",
            allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_tweet_id", nullable = false)
    private ProcessedTweet processedTweet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SentimentLabel label;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_strategy", nullable = false)
    private TokenStrategyLabel tokenStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_model_strategy", nullable = false)
    private SentimentStrategyLabel sentimentModelStrategy;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "analysis_date", nullable = false)
    private LocalDateTime analysisDate;
}
