package pl.bgnat.master.xsnts.exporter.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xsnts.exporter.service.CsvExportService;

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

    /** wyniki sentymentu */
    @GetMapping("/sentiment")
    public ResponseEntity<String> sentiment(@RequestParam(required = false) String path) throws Exception {
        return ResponseEntity.ok(csv.exportSentimentResults(path));
    }

    /** wyniki topic sentymentu **/
    @GetMapping("/topic-sentiment/{modelId}")
    public ResponseEntity<String> topicSentiment(
            @PathVariable Long modelId,
            @RequestParam(required = false) String path) throws Exception {
        return ResponseEntity.ok(csv.exportTopicSentimentTweets(modelId, path));
    }
}
