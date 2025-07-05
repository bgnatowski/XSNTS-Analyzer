package pl.bgnat.master.xscrapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicModelingRequest {
    private Integer numberOfTopics;
    private String poolingStrategy; // "hashtag", "author", "temporal"
    private Integer minDocumentSize; // Minimalna liczba tweetów w dokumencie
    private Integer maxIterations;
    private LocalDateTime startDate; // Filtrowanie tweetów od daty
    private LocalDateTime endDate;   // Filtrowanie tweetów do daty
    private String modelName;
}
