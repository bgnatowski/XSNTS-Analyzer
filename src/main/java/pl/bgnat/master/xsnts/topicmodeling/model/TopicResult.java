package pl.bgnat.master.xsnts.topicmodeling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encja przechowująca szczegóły pojedynczego tematu
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "TopicResult")
@Table(name = "topic_result")
public class TopicResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "topic_modeling_result_id", referencedColumnName = "id")
    private TopicModelingResult topicModelingResult;

    @Column(name = "topic_id", nullable = false)
    private Integer topicId;

    @Column(name = "topic_label", length = 200)
    private String topicLabel;

    @Column(name = "top_words", columnDefinition = "TEXT")
    private String topWords; // JSON z top słowami i ich wagami

    @Column(name = "word_count", nullable = false)
    private Integer wordCount;

    @Column(name = "document_count", nullable = false)
    private Integer documentCount; // Liczba dokumentów przypisanych do tematu

    @Column(name = "average_probability")
    private Double averageProbability; // Średnie prawdopodobieństwo występowania danego tematu w dokumentach, które utworzyły model
}
