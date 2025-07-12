package pl.bgnat.master.xsnts.normalization.service;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static org.springframework.util.StringUtils.*;


@Slf4j
@Service
public class LanguageDetectionService {

    private LanguageDetector detector;

    @PostConstruct
    public void initialize() {
        log.info("Inicjalizacja Language Detector dla wykrywania języka polskiego");

        detector = LanguageDetectorBuilder.fromLanguages(
                Language.POLISH,
                Language.ENGLISH,
                Language.GERMAN,
                Language.FRENCH,
                Language.SPANISH,
                Language.ITALIAN,
                Language.RUSSIAN,
                Language.UKRAINIAN
        ).withPreloadedLanguageModels().build();

        log.info("Language Detector zainicjalizowany pomyślnie");
    }

    @PreDestroy
    public void cleanup() {
        if (detector != null) {
            detector.unloadLanguageModels();
            log.info("Language Detector zwolnił zasoby");
        }
    }

    public boolean isPolish(String text) {
        if (!hasLength(text)) {
            return false;
        }

        try {
            log.debug("Zaczynam detekcje jezyka dla: {}", text);
            if (text.trim().length() < 10) {
                return detectShortText(text);
            }

            var detectedLanguage = detector.detectLanguageOf(text);
            log.debug("Language detected: " + detectedLanguage);
            return detectedLanguage == Language.POLISH;
        } catch (Exception e) {
            log.warn("Błąd podczas wykrywania języka dla tekstu: {}", e.getMessage());
            return false;
        }
    }

    private boolean detectShortText(String text) {
        text = text.toLowerCase();

        if (text.matches(".*[ąćęłńóśźż].*")) {
            return true;
        }

        return text.matches(".*(że|się|nie|już|też|oraz|przez|czyli|więc|jak|czy|gdzie|kiedy).*");
    }
}
