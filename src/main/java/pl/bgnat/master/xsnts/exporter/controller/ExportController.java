package pl.bgnat.master.xsnts.exporter.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xsnts.exporter.service.CsvExportService;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentRequest;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final CsvExportService csv;

    /** przetworzone tweety */
    @GetMapping("/processed")
    public ResponseEntity<String> processed(@RequestParam(required = false) String path) throws Exception {
        return ResponseEntity.ok(csv.exportProcessedTweets(path));
    }

    /** wyniki analizy tematów */
    @GetMapping("/topic-results/{modelId}")
    public ResponseEntity<String> processed(@PathVariable Long modelId, @RequestParam(required = false) String path) throws Exception {
        return ResponseEntity.ok(csv.exportTopicResults(modelId, path));
    }

    /** wyniki sentymentu z filtrowaniem według strategii */
    @GetMapping("/sentiment")
    public ResponseEntity<String> sentiment(
            @RequestBody SentimentRequest request,
            @RequestParam(required = false) String path) throws Exception {
        return ResponseEntity.ok(csv.exportSentimentResults(path, request));
    }

    /** wyniki topic sentymentu **/
    @GetMapping("/topic-sentiment/{modelId}")
    public ResponseEntity<String> topicSentiment(
            @PathVariable Long modelId,
            @RequestParam(required = false) String path) throws Exception {
        return ResponseEntity.ok(csv.exportTopicSentimentTweets(modelId, path));
    }
}
