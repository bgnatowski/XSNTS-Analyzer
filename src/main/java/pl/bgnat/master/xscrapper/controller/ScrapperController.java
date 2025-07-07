package pl.bgnat.master.xscrapper.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.bgnat.master.xscrapper.dto.UserCredential;
import pl.bgnat.master.xscrapper.service.scrapper.ScrapperService;

import java.util.List;

/**
 * Kontroler REST API do zarządzania operacjami scrapowania danych z platformy X.com.
 * Umożliwia uruchamianie zautomatyzowanych i manualnych zadań scrapujących.
 */
@Slf4j
@RestController
@RequestMapping("/api/scrapper")
@RequiredArgsConstructor
public class ScrapperController {

    private final ScrapperService scrapperService;

    /**
     * Uruchamia scrapowanie popularnych tweetów dla podanych słów kluczowych.
     * Operacje scrapowania są zazwyczaj asynchroniczne.
     *
     * @param keywords Lista słów kluczowych do wyszukania.
     * @return ResponseEntity z potwierdzeniem rozpoczęcia operacji.
     */
    @PostMapping("/popular")
    public ResponseEntity<String> scrapePopular(@RequestBody List<String> keywords) {
        log.info("Otrzymano żądanie zaplanowanego scrapowania popularnych tweetów dla słów kluczowych: {}", keywords);
        try {
            scrapperService.scheduledScrapePopularKeywords(keywords);
            return ResponseEntity.ok("Scrapowanie popularnych tweetów dla podanych słów kluczowych zostało uruchomione.");
        } catch (Exception e) {
            log.error("Błąd podczas uruchamiania zaplanowanego scrapowania popularnych tweetów: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Błąd podczas uruchamiania scrapowania: " + e.getMessage());
        }
    }

    /**
     * Uruchamia scrapowanie najnowszych tweetów dla podanych słów kluczowych.
     * Operacje scrapowania są zazwyczaj asynchroniczne.
     *
     * @param keywords Lista słów kluczowych do wyszukania.
     * @return ResponseEntity z potwierdzeniem rozpoczęcia operacji.
     */
    @PostMapping("/latest")
    public ResponseEntity<String> scrapeLatest(@RequestBody List<String> keywords) {
        log.info("Otrzymano żądanie zaplanowanego scrapowania najnowszych tweetów dla słów kluczowych: {}", keywords);
        try {
            scrapperService.scheduledScrapeLatestKeywords(keywords);
            return ResponseEntity.ok("Scrapowanie najnowszych tweetów dla podanych słów kluczowych zostało uruchomione.");
        } catch (Exception e) {
            log.error("Błąd podczas uruchamiania zaplanowanego scrapowania najnowszych tweetów: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Błąd podczas uruchamiania scrapowania: " + e.getMessage());
        }
    }

    /**
     * Manualnie uruchamia scrapowanie popularnych tweetów (zamiast wedlug harmonogramu).
     *
     * @return ResponseEntity z potwierdzeniem rozpoczęcia operacji.
     */
    @PostMapping("/manual/popular")
    public ResponseEntity<String> manualScrapePopular() {
        log.info("Otrzymano żądanie manualnego scrapowania popularnych tweetów.");
        try {
            scrapperService.scheduledScrapePopularKeywords();
            return ResponseEntity.ok("Manualne scrapowanie popularnych tweetów zostało uruchomione.");
        } catch (Exception e) {
            log.error("Błąd podczas manualnego scrapowania popularnych tweetów: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Błąd podczas uruchamiania scrapowania: " + e.getMessage());
        }
    }

    /**
     * Manualnie uruchamia scrapowanie najnowszych tweetów (zamiast harmonogramu).
     *
     * @return ResponseEntity z potwierdzeniem rozpoczęcia operacji.
     */
    @PostMapping("/manual/latest")
    public ResponseEntity<String> manualScrapeLatest() {
        log.info("Otrzymano żądanie manualnego scrapowania najnowszych tweetów.");
        try {
            scrapperService.scheduledScrapeLatestKeywords();
            return ResponseEntity.ok("Manualne scrapowanie najnowszych tweetów zostało uruchomione.");
        } catch (Exception e) {
            log.error("Błąd podczas manualnego scrapowania najnowszych tweetów: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Błąd podczas uruchamiania scrapowania: " + e.getMessage());
        }
    }

    /**
     * Manualnie uruchamia scrapowanie tablicy "Dla Ciebie" (For You).
     * Operacja jest asynchroniczna.
     *
     * @return ResponseEntity z potwierdzeniem rozpoczęcia operacji.
     */
    @PostMapping("/manual/for-you")
    public ResponseEntity<String> manualScrapeForYou() {
        log.info("Otrzymano żądanie manualnego scrapowania tablicy 'Dla Ciebie'.");
        try {
            scrapperService.scheduledScrapeForYouWallAsync();
            return ResponseEntity.ok("Manualne scrapowanie tablicy 'Dla Ciebie' zostało uruchomione asynchronicznie.");
        } catch (Exception e) {
            log.error("Błąd podczas manualnego scrapowania tablicy 'Dla Ciebie': {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Błąd podczas uruchamiania scrapowania: " + e.getMessage());
        }
    }

    // ========================================
    // SEKCJA 3: SCRAPING JEDNORAZOWY
    // ========================================

    /**
     * Uruchamia jednorazowe scrapowanie dla konkretnego słowa kluczowego, typu tablicy i użytkownika.
     * Jest to operacja synchroniczna, która może zająć więcej czasu.
     *
     * @param keyword  Słowo kluczowe do wyszukania.
     * @param wallType Typ tablicy do scrapowania (np. "popular", "latest").
     * @param user     Dane uwierzytelniające użytkownika (wstrzykiwane z ciała żądania).
     * @return ResponseEntity z potwierdzeniem rozpoczęcia operacji.
     */
    @PostMapping("/one")
    public ResponseEntity<String> scrapeOneByKeyword(
            @RequestParam String keyword,
            @RequestParam String wallType,
            @RequestBody UserCredential.User user) {
        log.info("Otrzymano żądanie jednorazowego scrapowania dla słowa kluczowego '{}', typ tablicy: '{}'", keyword, wallType);
        try {
            // Podstawowa walidacja parametrów
            if (keyword == null || keyword.trim().isEmpty() || wallType == null || wallType.trim().isEmpty()) {
                log.warn("Nieprawidłowe parametry żądania: keyword lub wallType jest pusty.");
                return ResponseEntity.badRequest().body("Słowo kluczowe i typ tablicy nie mogą być puste.");
            }
            scrapperService.scrapeOneByKeyword(keyword, wallType, user);
            return ResponseEntity.ok("Jednorazowe scrapowanie dla słowa kluczowego '" + keyword + "' zostało uruchomione.");
        } catch (Exception e) {
            log.error("Błąd podczas jednorazowego scrapowania dla słowa kluczowego '{}': {}", keyword, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Błąd podczas uruchamiania scrapowania: " + e.getMessage());
        }
    }
}
