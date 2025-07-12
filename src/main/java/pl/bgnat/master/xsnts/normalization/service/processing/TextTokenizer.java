package pl.bgnat.master.xsnts.normalization.service.processing;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.normalization.utils.PolishStemmerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasLength;
import static pl.bgnat.master.xsnts.normalization.utils.StopWordsManager.loadStopWords;

@Slf4j
@Component
public class TextTokenizer {
    private final Set<String> stopWords;

    public TextTokenizer() {
        log.info("Inicjalizacja TextTokenizer - rozpoczynam ładowanie wzorców i słów stop");

        this.stopWords = loadStopWords();

        log.info("TextTokenizer zainicjalizowany. Załadowano {} słów stop", stopWords.size());
    }
    /**
     * Tokenizuje znormalizowany tekst na listę tokenów.
     * Proces tokenizacji obejmuje:
     * 1. Podział tekstu na tokeny
     * 2. Filtrowanie tokenów minimalnej długości
     * 3. Usuwanie słów stop
     * 4. Stemming
     * 5. Walidację wynikowych tokenów
     *
     * @param normalizedText znormalizowany tekst do tokenizacji
     * @return lista tokenów
     */
    public List<String> tokenize(String normalizedText) {
        if (!hasLength(normalizedText)) {
            log.debug("Otrzymano pusty tekst do tokenizacji");
            return new ArrayList<>();
        }

        log.debug("Rozpoczynam tokenizację tekstu");

        List<String> tokens = Arrays.stream(normalizedText.split("\\s+"))
                .filter(this::isValidToken)
                .filter(this::isNotStopWord)
                .collect(Collectors.toList());

        log.debug("Tokenizacja zakończona. Wygenerowano {} tokenów", tokens.size());
        return tokens;
    }

    public List<String> lemmatizeTokens(List<String> tokens) {
        return tokens.stream().map(PolishStemmerUtil::lemmatize).toList();
    }

    private boolean isValidToken(String token) {
        return hasLength(token);
    }

    private boolean isNotStopWord(String token) {
        return !stopWords.contains(token.toLowerCase());
    }
}
