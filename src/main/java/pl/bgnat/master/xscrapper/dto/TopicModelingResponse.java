package pl.bgnat.master.xscrapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    private Double pmiScore;        // PMI
    private Double npmiScore;       // Normalized PMI
    private Double uciScore;        // UMass Coherence Index
    private String coherenceInterpretation; // Tekstowa interpretacja


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
