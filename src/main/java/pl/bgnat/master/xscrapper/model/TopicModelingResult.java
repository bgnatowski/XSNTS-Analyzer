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
    private String modelName; // Nazwa modelu, np. "LDA_20_topics_hashtag_pooling"

    @Column(name = "number_of_topics", nullable = false)
    private Integer numberOfTopics;

    @Column(name = "pooling_strategy", nullable = false)
    private String poolingStrategy; // "hashtag", "author", "temporal"

    @Column(name = "documents_count", nullable = false)
    private Integer documentsCount; // Liczba dokumentów po poolingu

    @Column(name = "original_tweets_count", nullable = false)
    private Integer originalTweetsCount; // Liczba oryginalnych tweetów

    @Column(name = "training_date", nullable = false)
    private LocalDateTime trainingDate;

    @Column(name = "coherence_score")
    private Double coherenceScore; // PMI score

    @Column(name = "perplexity")
    private Double perplexity;

    @Column(name = "model_path", length = 500)
    private String modelPath; // Ścieżka do zapisanego modelu MALLET

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ModelStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public enum ModelStatus {
        TRAINING, COMPLETED, FAILED
    }
}
