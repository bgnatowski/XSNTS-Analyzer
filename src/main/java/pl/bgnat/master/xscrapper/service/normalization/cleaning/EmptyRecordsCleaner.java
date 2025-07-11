package pl.bgnat.master.xscrapper.service.normalization.cleaning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.model.normalization.ProcessedTweet;
import pl.bgnat.master.xscrapper.repository.normalization.ProcessedTweetRepository;
import pl.bgnat.master.xscrapper.dto.normalization.CleanupResult;

import java.util.List;

/**
 * Komponent odpowiedzialny za czyszczenie pustych rekordów
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmptyRecordsCleaner {

    private final ProcessedTweetRepository processedTweetRepository;

    /**
     * Usuwa rekordy z pustymi polami
     * @return wynik operacji czyszczenia
     */
    public CleanupResult cleanupEmptyRecords() {
        log.info("Rozpoczynam usuwanie pustych rekordów z processed_tweet");

        try {
            long emptyRecordsCount = getEmptyRecordsCount();
            log.info("Znaleziono {} pustych rekordów do usunięcia", emptyRecordsCount);

            if (emptyRecordsCount == 0) {
                return createSuccessResult(0, "Brak pustych rekordów do usunięcia");
            }

            int deletedCount = processedTweetRepository.deleteEmptyRecords();
            log.info("Usunięto {} pustych rekordów", deletedCount);

            return createSuccessResult(deletedCount,
                    "Pomyślnie usunięto " + deletedCount + " pustych rekordów");

        } catch (Exception e) {
            log.error("Błąd podczas usuwania pustych rekordów: {}", e.getMessage());
            return createErrorResult("Błąd podczas usuwania: " + e.getMessage());
        }
    }

    /**
     * Zwraca listę pustych rekordów
     * @return lista pustych rekordów
     */
    public List<ProcessedTweet> getEmptyRecords() {
        return processedTweetRepository.findEmptyRecords();
    }

    /**
     * Zwraca liczbę pustych rekordów
     * @return liczba pustych rekordów
     */
    public long getEmptyRecordsCount() {
        return processedTweetRepository.countEmptyRecords();
    }

    private CleanupResult createSuccessResult(int deletedCount, String message) {
        return CleanupResult.builder()
                .deletedRecords(deletedCount)
                .success(true)
                .message(message)
                .build();
    }

    private CleanupResult createErrorResult(String message) {
        return CleanupResult.builder()
                .deletedRecords(0)
                .success(false)
                .message(message)
                .build();
    }
}
