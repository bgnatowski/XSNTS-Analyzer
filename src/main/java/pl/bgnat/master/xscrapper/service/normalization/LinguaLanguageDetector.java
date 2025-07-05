package pl.bgnat.master.xscrapper.service.normalization;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LinguaLanguageDetector {

    private LanguageDetector detector;

    @PostConstruct
    void init() {
        detector = LanguageDetectorBuilder
                .fromLanguages(Language.POLISH, Language.ENGLISH)
                .withPreloadedLanguageModels()
                .build();
        log.info("LinguaLanguageDetector załadowany.");
    }

    /** @return true jeżeli tekst jest prawdopodobnie polski */
    public boolean isPolish(String text) {
        if (text == null || text.isBlank()) return false;
        Language lang = detector.detectLanguageOf(text);
        return lang == Language.POLISH;
    }
}
