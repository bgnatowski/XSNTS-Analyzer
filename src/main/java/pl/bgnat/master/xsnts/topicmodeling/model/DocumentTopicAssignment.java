package pl.bgnat.master.xsnts.topicmodeling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encja przechowująca przypisania dokumentów (grup tweetów) do tematów
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "DocumentTopicAssignment")
@Table(name = "document_topic_assignment")
public class DocumentTopicAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "topic_modeling_result_id", referencedColumnName = "id")
    private TopicModelingResult topicModelingResult;

    @Column(name = "document_id", nullable = false)
    private String documentId; // Identyfikator dokumentu (np. hashtag lub user)

    @Column(name = "document_type", nullable = false)
    private String documentType; // "hashtag", "user", "temporal"

    @Column(name = "dominant_topic_id", nullable = false)
    private Integer dominantTopicId;

    @Column(name = "topic_probabilities", columnDefinition = "TEXT")
    private String topicProbabilities; // JSON z prawdopodobieństwami dla wszystkich tematów

    @Column(name = "tweet_ids", columnDefinition = "TEXT")
    private String tweetIds; // JSON z ID tweetów w tym dokumencie

    @Column(name = "tweets_count", nullable = false)
    private Integer tweetsCount;
}
