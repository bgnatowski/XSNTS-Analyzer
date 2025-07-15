package pl.bgnat.master.xsnts.topicmodeling.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xsnts.topicmodeling.dto.TopicModelingRequest;
import pl.bgnat.master.xsnts.topicmodeling.dto.TopicModelingResponse;
import pl.bgnat.master.xsnts.topicmodeling.service.MalletTopicModelingService;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.format.DateTimeFormatter.*;

/**
 * Kontroler REST API dla topic modeling
 */
@Slf4j
@RestController
@RequestMapping("/api/topic-modeling")
@RequiredArgsConstructor
public class TopicModelingController {

    private final MalletTopicModelingService topicModelingService;

    /**
     * Uruchamia nowy proces topic modeling
     */
    @PostMapping("/lda/train")
    public ResponseEntity<TopicModelingResponse> trainModel(@RequestBody TopicModelingRequest request) {
        log.info("Otrzymano żądanie trenowania modelu: {} tematów", request.getNumberOfTopics());

        try {
            if (request.getNumberOfTopics() == null || request.getNumberOfTopics() < 2) {
                return ResponseEntity.badRequest().build();
            }

            if (request.getPoolingStrategy() == null) {
                request.setPoolingStrategy("hashtag");
            }

            TopicModelingResponse response = topicModelingService.performTopicModeling(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Błąd podczas trenowania modelu: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Pobiera listę dostępnych modeli
     */
    @GetMapping("/models")
    public ResponseEntity<List<TopicModelingResponse>> getAvailableModels() {
        try {
            List<TopicModelingResponse> models = topicModelingService.getAvailableModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Błąd podczas pobierania listy modeli: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Pobiera szczegóły konkretnego modelu
     */
    @GetMapping("/models/{modelId}")
    public ResponseEntity<TopicModelingResponse> getModelDetails(@PathVariable Long modelId) {
        try {
            TopicModelingResponse response = topicModelingService.getModelDetails(modelId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Błąd podczas pobierania szczegółów modelu {}: {}", modelId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}
