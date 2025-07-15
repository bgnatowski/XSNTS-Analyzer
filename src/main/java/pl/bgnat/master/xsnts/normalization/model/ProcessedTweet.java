package pl.bgnat.master.xsnts.normalization.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;

import java.time.LocalDateTime;

/**
    Encja przechowująca znormalizowane tweety i ich tokeny
*/
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ProcessedTweet")
@Table(name = "processed_tweet")
public class ProcessedTweet {

    @Id
    @SequenceGenerator(
            name = "processed_tweet_id_generator",
            sequenceName = "processed_tweet_id_generator",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "processed_tweet_id_generator")
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "tweet_id", referencedColumnName = "id")
    private Tweet originalTweet;

    @Column(name = "normalized_content", length = 4000)
    private String normalizedContent;

    @Column(name = "tokens", columnDefinition = "TEXT")
    private String tokens;

    @Column(name = "tokens_lemmatized", columnDefinition = "TEXT")
    private String tokensLemmatized;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "token_count")
    private Integer tokenCount;
}
