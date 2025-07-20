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
    private String topicModel;          // LDA - w przyszłości możliwe inne
    private boolean isUseBigrams;       // Czy używać bigramów
    private Integer numberOfTopics;     // Ustawienie ilości topiców K - pozostałość
    private String poolingStrategy;     // "hashtag" / "temporal"
    private Integer minDocumentSize;    // Minimalna liczba tweetów w dokumencie
    private Integer maxIterations;      // Ustawienie ilości iteracji
    private LocalDateTime startDate;    // Filtrowanie tweetów od daty
    private LocalDateTime endDate;      // Filtrowanie tweetów do daty
    private String modelName;           // Ustawienie nazwy modelu
    private boolean skipMentions;       // Czy zostawiać @anonimized
}
