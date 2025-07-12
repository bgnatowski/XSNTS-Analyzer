package pl.bgnat.master.xsnts.sentiment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xsnts.sentiment.model.TopicSentimentStats;
import pl.bgnat.master.xsnts.sentiment.service.SentimentAnalysisService;
import pl.bgnat.master.xsnts.sentiment.service.TopicSentimentAnalysisService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
@RequiredArgsConstructor
public class SentimentController {

    private final SentimentAnalysisService service;
    private final TopicSentimentAnalysisService analysisService;

    /** endpoint batch – zwraca liczbę nowych wyników */
    @PostMapping("/analyze-all")
    public ResponseEntity<Map<String, Object>> analyzeAll() {
        int created = service.analyzeAll();
        return ResponseEntity.ok(
                Map.of("insertedResults", created,
                        "message", "Batch sentiment analysis completed"));
    }

    @GetMapping("/{modelId}/stats")
    public ResponseEntity<List<TopicSentimentStats>> getStats(@PathVariable Long modelId) {
        return ResponseEntity.ok(analysisService.getSentimentStatsForModel(modelId));
    }
}
