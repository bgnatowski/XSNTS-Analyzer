package pl.bgnat.master.xscrapper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xscrapper.service.export.CsvExportService;

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

    /** wyniki sentymentu */
    @GetMapping("/sentiment")
    public ResponseEntity<String> sentiment(@RequestParam(required = false) String path) throws Exception {
        return ResponseEntity.ok(csv.exportSentimentResults(path));
    }
}
