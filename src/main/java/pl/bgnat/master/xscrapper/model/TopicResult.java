package pl.bgnat.master.xscrapper.model;

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
    private String topicLabel; // Automatycznie wygenerowana etykieta

    @Column(name = "top_words", columnDefinition = "TEXT")
    private String topWords; // JSON z top słowami i ich wagami

    @Column(name = "word_count", nullable = false)
    private Integer wordCount;

    @Column(name = "document_count", nullable = false)
    private Integer documentCount; // Liczba dokumentów przypisanych do tematu

    @Column(name = "average_probability")
    private Double averageProbability;

    @Column(name = "pmi_coherence")
    private Double pmiCoherence;       // UCI-PMI (avg PMI)

    @Column(name = "npmi_coherence")
    private Double npmiCoherence;      // NPMI

    @Column(name = "uci_coherence")
    private Double uciCoherence;       // UCI

    @Column(name = "umass_coherence")
    private Double umassCoherence;     // UMass

}
