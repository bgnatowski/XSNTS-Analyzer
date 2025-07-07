package pl.bgnat.master.xscrapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import java.time.LocalDateTime;

/**
 * DTO requestu topic modelingu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicModelingRequest {
    private Integer numberOfTopics;     // Ustawienie ilosci topicow
    private String poolingStrategy;     // "hashtag", "author", "temporal"
    private Integer minDocumentSize;    // Minimalna liczba tweetów w dokumencie
    private Integer maxIterations;      // Ustawienie ilosci iteracji
    private LocalDateTime startDate;    // Filtrowanie tweetów od daty
    private LocalDateTime endDate;      // Filtrowanie tweetów do daty
    private String modelName;           // Ustawienie nazwy modelu
}
