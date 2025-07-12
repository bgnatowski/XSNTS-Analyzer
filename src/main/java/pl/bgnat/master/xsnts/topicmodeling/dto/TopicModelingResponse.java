package pl.bgnat.master.xsnts.topicmodeling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO zawierające model response'a dla topic modelingu
 */
@Data
@Builder
public class TopicModelingResponse {
    private Long modelId;
    private String modelName;
    private Integer numberOfTopics;
    private String poolingStrategy;
    private Integer documentsCount;
    private Integer originalTweetsCount;
    private LocalDateTime trainingDate;
    private Double perplexity;
    private String status;
    private List<TopicSummary> topics;

    private Double pmiScore;            // PMI
    private Double npmiScore;           // NPMI (-1, 1)
    private Double uciScore;            // Uci Coherence Index
    private Double umassScore;          // UMass Coherence Index
    private String pmiInterpretation;   // Tekstowa interpretacja wyniku pmi
    private String umassInterpretation; // Tekstowa interpretacja wyniku umass


    @Data
    @Builder
    public static class TopicSummary {
        private Integer topicId;
        private String topicLabel;
        private List<WordWeight> topWords;
        private Integer documentCount;
        private Double averageProbability;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordWeight {
        private String word;
        private Double weight;
    }
}
