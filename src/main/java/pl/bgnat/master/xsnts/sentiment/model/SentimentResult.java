package pl.bgnat.master.xsnts.sentiment.model;

import jakarta.persistence.*;
import lombok.*;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;

import java.time.LocalDateTime;

@Entity
@Table(name = "sentiment_result",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"processed_tweet_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SentimentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "sentiment_result_seq")
    @SequenceGenerator(name = "sentiment_result_seq",
            sequenceName = "sentiment_result_seq",
            allocationSize = 1)
    private Long id;

    @OneToOne
    @JoinColumn(name = "processed_tweet_id",
            referencedColumnName = "id",
            nullable = false)
    private ProcessedTweet processedTweet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SentimentLabel label;

    @Column(name = "score", nullable = false)
    private double score;               // surowy wynik (suma polaryzacji)

    @Column(name = "analysis_date", nullable = false)
    private LocalDateTime analysisDate;
}
