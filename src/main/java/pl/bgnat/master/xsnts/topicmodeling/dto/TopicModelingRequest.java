package pl.bgnat.master.xsnts.topicmodeling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO requestu topic modelingu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicModelingRequest {
    private String tokenStrategy;       // "normal" / "lemmatized"
    private String topicModel;          // np. LDA (na razie tylko tyle)
    private boolean isUseBigrams;       // czy uzywac bigramow
    private Integer numberOfTopics;     // Ustawienie ilosci topicow
    private String poolingStrategy;     // "hashtag", "temporal", tbd. "author"
    private Integer minDocumentSize;    // Minimalna liczba tweetów w dokumencie
    private Integer maxIterations;      // Ustawienie ilosci iteracji
    private LocalDateTime startDate;    // Filtrowanie tweetów od daty
    private LocalDateTime endDate;      // Filtrowanie tweetów do daty
    private String modelName;           // Ustawienie nazwy modelu
    private boolean skipMentions;       // true sprawia ze do tworzenia dokumentu nie bierze @anonimized
}
