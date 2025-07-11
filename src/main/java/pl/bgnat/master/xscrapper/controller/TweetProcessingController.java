package pl.bgnat.master.xscrapper.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xscrapper.dto.normalization.CleanupResult;
import pl.bgnat.master.xscrapper.dto.normalization.ProcessingStatsDTO;
import pl.bgnat.master.xscrapper.model.normalization.ProcessedTweet;
import pl.bgnat.master.xscrapper.service.normalization.TweetProcessingService;

import java.io.IOException;
import java.util.List;

/**
 * Kontroler REST API dla przetwarzania tweetów
 * Obsługuje operacje normalizacji, tokenizacji i zarządzania przetworzonymi danymi
 */
@Slf4j
@RestController
@RequestMapping("/api/processing")
@RequiredArgsConstructor
public class TweetProcessingController {

    private final TweetProcessingService processingService;

    /**
     * Uruchamia przetwarzanie wszystkich tweetów z bazy danych
     * Wykonuje normalizację i tokenizację treści tweetów
     *
     * @return ResponseEntity z informacją o rozpoczęciu przetwarzania
     */
    @PostMapping("/process-all")
    public ResponseEntity<String> processAllTweets() {
        log.info("Otrzymano żądanie przetwarzania wszystkich tweetów");

        try {
            log.info("Przetwarzanie wszystkich tweetów rozpoczęte pomyślnie");
            String processedResult = processingService.processAllTweets();
            return ResponseEntity.ok(processedResult);

        } catch (Exception e) {
            log.error("Błąd podczas uruchamiania przetwarzania tweetów: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Błąd podczas przetwarzania: " + e.getMessage());
        }
    }

    /**
     * Zwraca statystyki przetwarzania
     * Pokazuje liczbę przetworzonych tweetów, postęp i średnie wartości
     *
     * @return ResponseEntity z obiektiem ProcessingStats
     */
    @GetMapping("/stats")
    public ResponseEntity<ProcessingStatsDTO> getProcessingStats() {
        log.debug("Pobieranie statystyk przetwarzania");

        try {
            ProcessingStatsDTO stats = processingService.getProcessingStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Błąd podczas pobierania statystyk: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * Zwraca liczbę pustych rekordów w tabeli processed_tweet
     * Puste rekordy to te, które mają puste pola normalized_content lub tokens
     *
     * @return ResponseEntity z liczbą pustych rekordów
     */
    @GetMapping("/empty-count")
    public ResponseEntity<Long> getEmptyRecordsCount() {
        log.debug("Pobieranie liczby pustych rekordów");

        try {
            long count = processingService.getEmptyRecordsCount();
            log.info("Znaleziono {} pustych rekordów", count);
            return ResponseEntity.ok(count);

        } catch (Exception e) {
            log.error("Błąd podczas pobierania liczby pustych rekordów: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(0L);
        }
    }

    /**
     * Zwraca listę pustych rekordów (do podglądu przed usunięciem)
     * Pozwala na weryfikację, które rekordy zostaną usunięte
     *
     * @return ResponseEntity z listą pustych rekordów ProcessedTweet
     */
    @GetMapping("/empty-records")
    public ResponseEntity<List<ProcessedTweet>> getEmptyRecords() {
        log.debug("Pobieranie listy pustych rekordów");

        try {
            List<ProcessedTweet> emptyRecords = processingService.getEmptyRecords();
            log.info("Znaleziono {} pustych rekordów do wyświetlenia", emptyRecords.size());
            return ResponseEntity.ok(emptyRecords);

        } catch (Exception e) {
            log.error("Błąd podczas pobierania pustych rekordów: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    /**
     * Usuwa rekordy z pustymi polami normalized_content i tokens
     * Operacja jest nieodwracalna - usuwa dane z bazy danych
     *
     * @return ResponseEntity z wynikiem operacji czyszczenia (CleanupResult)
     */
    @DeleteMapping("/cleanup-empty")
    public ResponseEntity<CleanupResult> cleanupEmptyRecords() {
        log.info("Otrzymano żądanie usunięcia pustych rekordów");

        try {
            CleanupResult result = processingService.cleanupEmptyRecords();

            if (result.getSuccess()) {
                log.info("Pomyślnie usunięto {} pustych rekordów", result.getDeletedRecords());
                return ResponseEntity.ok(result);
            } else {
                log.warn("Nie udało się usunąć pustych rekordów: {}", result.getMessage());
                return ResponseEntity.internalServerError().body(result);
            }

        } catch (Exception e) {
            log.error("Nieoczekiwany błąd podczas usuwania pustych rekordów: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(CleanupResult.builder()
                            .deletedRecords(0)
                            .success(false)
                            .message("Nieoczekiwany błąd: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/non-polish-count")
    public ResponseEntity<Long> getNonPolishTweetsCount() {
        log.info("Otrzymano żądanie liczenia tweetów niepolskich");

        try {
            long count = processingService.countNonPolishTweets();
            log.info("Znaleziono {} tweetów niepolskich", count);
            return ResponseEntity.ok(count);

        } catch (Exception e) {
            log.error("Błąd podczas liczenia tweetów niepolskich: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(0L);
        }
    }

    @DeleteMapping("/cleanup-non-polish")
    public ResponseEntity<CleanupResult> cleanupNonPolishTweets() {
        log.info("Otrzymano żądanie usunięcia tweetów niepolskich");

        try {
            CleanupResult result = processingService.cleanupNonPolishTweets();

            if (result.getSuccess()) {
                log.info("Pomyślnie usunięto {} tweetów niepolskich", result.getDeletedRecords());
                return ResponseEntity.ok(result);
            } else {
                log.warn("Nie udało się usunąć tweetów niepolskich: {}", result.getMessage());
                return ResponseEntity.internalServerError().body(result);
            }

        } catch (Exception e) {
            log.error("Nieoczekiwany błąd podczas usuwania tweetów niepolskich: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(CleanupResult.builder()
                            .deletedRecords(0)
                            .success(false)
                            .message("Nieoczekiwany błąd: " + e.getMessage())
                            .build());
        }
    }
}
