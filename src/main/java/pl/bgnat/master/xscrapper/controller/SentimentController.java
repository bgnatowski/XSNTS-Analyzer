package pl.bgnat.master.xscrapper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xscrapper.model.sentiment.SentimentResult;
import pl.bgnat.master.xscrapper.service.sentiment.SentimentAnalysisService;

import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
@RequiredArgsConstructor
public class SentimentController {

    private final SentimentAnalysisService service;

    /** endpoint batch – zwraca liczbę nowych wyników */
    @PostMapping("/analyze-all")
    public ResponseEntity<Map<String, Object>> analyzeAll() {
        int created = service.analyzeAll();
        return ResponseEntity.ok(
                Map.of("insertedResults", created,
                        "message", "Batch sentiment analysis completed"));
    }
}
