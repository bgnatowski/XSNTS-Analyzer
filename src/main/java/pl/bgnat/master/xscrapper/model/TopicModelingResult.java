package pl.bgnat.master.xscrapper.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Encja przechowująca wyniki analizy tematów dla grupowanych tweetów
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "TopicModelingResult")
@Table(name = "topic_modeling_result")
public class TopicModelingResult {

    @Id
    @SequenceGenerator(
            name = "topic_modeling_id_generator",
            sequenceName = "topic_modeling_id_generator",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "topic_modeling_id_generator")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "number_of_topics", nullable = false)
    private Integer numberOfTopics;

    @Column(name = "pooling_strategy", nullable = false)
    private String poolingStrategy; // "hashtag", "author", "temporal"

    @Builder.Default
    @Column(name = "documents_count", nullable = false)
    private Integer documentsCount = 0;

    @Builder.Default
    @Column(name = "original_tweets_count", nullable = false)
    private Integer originalTweetsCount = 0;

    @Column(name = "training_date", nullable = false)
    private LocalDateTime trainingDate;

    @Column(name = "pmi")
    private Double pmi;

    @Column(name = "npmi")
    private Double npmi;

    @Column(name = "uci")
    private Double uci;

    @Column(name = "umass")
    private Double umass;

    @Column(name = "coherenceInterpretation")
    private String coherenceInterpretation;

    @Column(name = "umassInterpretation")
    private String umassInterpretation;

    @Column(name = "perplexity")
    private Double perplexity;

    @Column(name = "model_path", length = 500)
    private String modelPath;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ModelStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public enum ModelStatus {
        TRAINING, COMPLETED, FAILED
    }
}
