package pl.bgnat.master.xsnts.sentiment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentRequest;
import pl.bgnat.master.xsnts.sentiment.dto.TopicSentimentStats;
import pl.bgnat.master.xsnts.sentiment.service.SentimentAnalysisService;
import pl.bgnat.master.xsnts.sentiment.service.TopicSentimentAnalysisService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
@RequiredArgsConstructor
public class SentimentController {

    private final SentimentAnalysisService sentimentAnalysisService;
    private final TopicSentimentAnalysisService topicSentimentAnalysisService;

    /** endpoint batch - analiza wszyskich processed tweets */
    @PostMapping("/analyze-all")
    public ResponseEntity<Map<String, Object>> analyzeAll(@RequestBody SentimentRequest request) {
        int created = sentimentAnalysisService.analyzeAll(request);
        return ResponseEntity.ok(Map.of("Utworzono:", created, "message", "Analiza sentymentu zakończona."));
    }


    /** Na podstawie przeanalizowanych SentimentResult wylicza sentyment poszczególnych Topiców według modelId*/
    @GetMapping("/{modelId}/stats")
    public List<TopicSentimentStats> stats(@PathVariable Long modelId,
                                           @RequestBody SentimentRequest request) {
        return topicSentimentAnalysisService.getSentimentStatsForModel(modelId, request);
    }

}
