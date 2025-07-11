package pl.bgnat.master.xscrapper.service.normalization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.dto.normalization.NormalizedTweet;
import pl.bgnat.master.xscrapper.utils.normalization.PolishStemmerUtil;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasLength;
import static pl.bgnat.master.xscrapper.utils.normalization.PatternMatcher.*;
import static pl.bgnat.master.xscrapper.utils.normalization.StopWordsManager.loadStopWords;

/**
 * Komponent odpowiedzialny za normalizację i tokenizację tekstu tweetów.
 * Implementuje proces wstępnego przetwarzania tekstu zgodnie z praktykami
 * dla analizy treści mediów społecznościowych, w szczególności Twittera/X.
 */
@Slf4j
@Component
public class TextNormalizer {
    private static final String USER_PLACEHOLDER = "ANONIMIZED";
    private static final String NUMBER_PLACEHOLDER = " NUMBER ";

    private final Pattern urlPattern;
    private final Pattern mentionPattern;
    private final Pattern numberPattern;
    private final Pattern punctuationPattern;
    private final Pattern whitespacePattern;

    private final Set<String> stopWords;

    public TextNormalizer() {
        log.info("Inicjalizacja TextNormalizer - rozpoczynam ładowanie wzorców i słów stop");

        this.stopWords = loadStopWords();
        this.urlPattern = createUrlPattern();
        this.mentionPattern = createMentionPattern();
        this.numberPattern = createNumberPattern();
        this.punctuationPattern = createPunctuationPattern();
        this.whitespacePattern = createWhitespacePattern();

        log.info("TextNormalizer zainicjalizowany. Załadowano {} słów stop", stopWords.size());
    }

    /**
     * Wykonuje pełen proces normalizacji i tokenizacji tekstu.
     */
    public NormalizedTweet normalize(String text) {
        validateInput(text);

        log.debug("Rozpoczynam pełne przetwarzanie tekstu");

        try {
            String normalized = normalizeText(text);
            List<String> tokens = tokenize(normalized);
            List<String> tokensLemmatized = lemmatizeTokens(tokens);

            NormalizedTweet result = NormalizedTweet.builder()
                    .normalizedContent(normalized)
                    .tokens(tokens)
                    .tokensLemmatized(tokensLemmatized)
                    .tokenCount(tokens.size())
                    .build();

            log.debug("Przetwarzanie zakończone pomyślnie. Tokens: {}", tokens.size());
            return result;

        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania tekstu: {}", e.getMessage(), e);
            return createEmptyResult();
        }
    }

    /**
     * Normalizuje tekst tweeta usuwając szum informacyjny i standaryzując format.
     * Proces normalizacji obejmuje:
     * 1. Usuwanie URL-i
     * 2. Normalizację wzmianek (@username) - anonimizacja lub usuwanie
     * 3. Zamianę liczb długich liczb na placeholdery
     * 4. Konwersję na małe litery
     * 5. Normalizację znaków Unicode
     * 6. Usuwanie interpunkcji
     * 7. Normalizację białych znaków
     *
     * @param text tekst do normalizacji
     * @return znormalizowany tekst
     */
    public String normalizeText(String text) {
        if (!hasLength(text)) {
            log.warn("Otrzymano pusty lub null tekst do normalizacji");
            return "";
        }

        log.debug("Rozpoczynam normalizację tekstu o długości: {}", text.length());

        String normalized = text;

        // Krok 1: Usunięcie URL-i
        normalized = removeUrls(normalized);

        // Krok 2: Normalizacja wzmianek użytkowników
        normalized = normalizeMentions(normalized);

        // Krok 3: Normalizacja liczb
        normalized = normalizeNumbers(normalized);

        // Krok 4: Konwersja na małe litery
        normalized = normalized.toLowerCase();

        // Krok 5: Normalizacja Unicode
        normalized = normalizeUnicode(normalized);

        // Krok 6: Usunięcie interpunkcji
        normalized = removePunctuation(normalized);

        // Krok 7: Normalizacja białych znaków (wielokrotne spacje)
        normalized = normalizeWhitespace(normalized);

        log.debug("Zakończono normalizację. Długość po przetworzeniu: {}", normalized.length());
        return normalized.trim();
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
            log.warn("Otrzymano pusty tekst do tokenizacji");
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

    private static List<String> lemmatizeTokens(List<String> tokens) {
        return tokens.stream().map(PolishStemmerUtil::lemmatize).toList();
    }

    /**
     * Usuwa URL-e z tekstu zastępując je spacją.
     * URL-e w tweetach nie niosą wartości semantycznej dla analizy treści.
     */
    private String removeUrls(String text) {
        return urlPattern.matcher(text).replaceAll(" ");
    }

    /**
     * Normalizuje wzmianki użytkowników (@username) zastępując je placeholderem.
     * Placeholder jako pusty, bo anonimizacja wprowadzała szum informacyjny dla zebranych tweetow
     * można edytować w celu badan i usuwać na poziomie analizy topicu / sentymentu
     */
    private String normalizeMentions(String text) {
        return mentionPattern.matcher(text).replaceAll(USER_PLACEHOLDER);
    }

    /**
     * Zastępuje liczby dłuższe niż 5 znaków placeholder NUMBER (zostawia daty typu 2025).
     * Konkretne wartości liczbowe rzadko mają znaczenie dla analizy.
     */
    private String normalizeNumbers(String text) {
        return numberPattern.matcher(text).replaceAll(NUMBER_PLACEHOLDER);
    }

    /**
     * Normalizuje znaki Unicode zachowując polskie znaki diakrytyczne.
     */
    private String normalizeUnicode(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    /**
     * Usuwa interpunkcję zachowując litery, cyfry i spacje.
     */
    private String removePunctuation(String text) {
        return punctuationPattern.matcher(text).replaceAll(" ");
    }

    /**
     * Normalizuje białe znaki zastępując wielokrotne spacje pojedynczą spacją.
     */
    private String normalizeWhitespace(String text) {
        return whitespacePattern.matcher(text).replaceAll(" ");
    }

    private boolean isValidToken(String token) {
        return hasLength(token);
    }

    private boolean isNotStopWord(String token) {
        return !stopWords.contains(token.toLowerCase());
    }

    private void validateInput(String text) {
        if (!hasLength(text)) {
            throw new IllegalArgumentException("Tekst do przetworzenia nie może być null");
        }
    }

    private NormalizedTweet createEmptyResult() {
        return NormalizedTweet.builder()
                .normalizedContent("")
                .tokens(new ArrayList<>())
                .tokenCount(0)
                .build();
    }
}
