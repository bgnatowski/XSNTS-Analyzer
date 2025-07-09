package pl.bgnat.master.xscrapper.service.normalization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.dto.NormalizedTweet;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Komponent odpowiedzialny za normalizację i tokenizację tekstu tweetów.
 * <p>
 * Implementuje proces wstępnego przetwarzania tekstu zgodnie z najlepszymi praktykami
 * dla analizy treści mediów społecznościowych, w szczególności Twittera/X.
 *
 * @author Bartosz Gnatowski
 * @version 1.3
 * @since 2025
 */
@Slf4j
@Component
public class TextNormalizer {

    // ========================================
    // STAŁE KONFIGURACYJNE
    // ========================================

    private static final String USER_PLACEHOLDER = " ";
    private static final String NUMBER_PLACEHOLDER = " NUMBER ";

    // ========================================
    // WZORCE REGEX DLA NORMALIZACJI
    // ========================================

    private final Pattern urlPattern;
    private final Pattern mentionPattern;
//    private final Pattern hashtagPattern;
    private final Pattern numberPattern;
    private final Pattern punctuationPattern;
    private final Pattern whitespacePattern;

    // ========================================
    // KONFIGURACJA I DANE
    // ========================================

    private final Set<String> stopWords;
    private final StopWordsManager stopWordsManager;
    private final PatternMatcher patternMatcher;

    @Value("${app.nlp.min-token-length:1}")
    private int minTokenLength;

    @Value("${app.nlp.preserve-hashtag-content:true}")
    private boolean preserveHashtagContent;

    public TextNormalizer() {
        log.info("Inicjalizacja TextNormalizer - rozpoczynam ładowanie wzorców i słów stop");

        this.stopWordsManager = new StopWordsManager();
        this.patternMatcher = new PatternMatcher();

        this.stopWords = stopWordsManager.loadStopWords();
        this.urlPattern = patternMatcher.createUrlPattern();
        this.mentionPattern = patternMatcher.createMentionPattern();
//        this.hashtagPattern = patternMatcher.createHashtagPattern();
        this.numberPattern = patternMatcher.createNumberPattern();
        this.punctuationPattern = patternMatcher.createPunctuationPattern();
        this.whitespacePattern = patternMatcher.createWhitespacePattern();

        log.info("TextNormalizer zainicjalizowany. Załadowano {} słów stop", stopWords.size());
    }

    // ========================================
    // GŁÓWNE METODY PUBLICZNE
    // ========================================

    /**
     * Normalizuje tekst tweeta usuwając hałas i standaryzując format.
     * <p>
     * Proces normalizacji obejmuje:
     * 1. Usuwanie URL-i
     * 2. Normalizację wzmianek (@username)
     * 4. Zamianę liczb na placeholdery
     * 5. Konwersję na małe litery
     * 6. Normalizację znaków Unicode
     * 7. Usuwanie interpunkcji
     * 8. Normalizację białych znaków
     *
     * @param text tekst do normalizacji
     * @return znormalizowany tekst
     */
    public String normalizeText(String text) {
        if (!isValidInput(text)) {
            log.debug("Otrzymano pusty lub null tekst do normalizacji");
            return "";
        }

        log.debug("Rozpoczynam normalizację tekstu o długości: {}", text.length());

        String normalized = text;

        // Krok 1: Usunięcie URL-i (zgodnie z zaleceniami literatury NLP dla social media)
        normalized = removeUrls(normalized);

        // Krok 2: Normalizacja wzmianek użytkowników
        normalized = normalizeMentions(normalized);

        // Krok 4: Normalizacja liczb
        normalized = normalizeNumbers(normalized);

        // Krok 5: Przetwarzanie emotikonów
        normalized = processEmoticons(normalized);

        // Krok 6: Konwersja na małe litery
        normalized = normalized.toLowerCase();

        // Krok 7: Normalizacja Unicode
        normalized = normalizeUnicode(normalized);

        // Krok 8: Usunięcie interpunkcji
        normalized = removePunctuation(normalized);

        // Krok 9: Normalizacja białych znaków
        normalized = normalizeWhitespace(normalized);

        log.debug("Zakończono normalizację. Długość po przetworzeniu: {}", normalized.length());
        return normalized.trim();
    }

    /**
     * Tokenizuje znormalizowany tekst na listę tokenów.
     * <p>
     * Proces tokenizacji obejmuje:
     * 1. Podział tekstu na tokeny
     * 2. Filtrowanie tokenów minimalnej długości
     * 3. Usuwanie słów stop
     * 4. Walidację wynikowych tokenów
     *
     * @param normalizedText znormalizowany tekst do tokenizacji
     * @return lista tokenów
     */
    public List<String> tokenize(String normalizedText) {
        if (!isValidInput(normalizedText)) {
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

    /**
     * Wykonuje pełen proces normalizacji i tokenizacji tekstu.
     *
     * @param text tekst do przetworzenia
     * @return obiekt NormalizedTweet z wynikami przetwarzania
     */
    public NormalizedTweet processText(String text) {
        validateProcessingInput(text);

        log.debug("Rozpoczynam pełne przetwarzanie tekstu");

        try {
            String normalized = normalizeText(text);
            List<String> tokens = tokenize(normalized);

            NormalizedTweet result = NormalizedTweet.builder()
                    .normalizedContent(normalized)
                    .tokens(tokens)
                    .tokenCount(tokens.size())
                    .build();

            log.debug("Przetwarzanie zakończone pomyślnie. Tokens: {}", tokens.size());
            return result;

        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania tekstu: {}", e.getMessage(), e);
            return createEmptyResult();
        }
    }

    // ========================================
    // METODY NORMALIZACJI - PRYWATNE
    // ========================================

    /**
     * Usuwa URL-e z tekstu zastępując je spacją.
     * URLs w tweetach nie niosą wartości semantycznej dla analizy treści.
     */
    private String removeUrls(String text) {
        return urlPattern.matcher(text).replaceAll(" ");
    }

    /**
     * Normalizuje wzmianki użytkowników (@username) zastępując je placeholderem pustym.
     * Zachowuje informację o wzmiankach bez ujawniania konkretnych nazw użytkowników.
     */
    private String normalizeMentions(String text) {
        return mentionPattern.matcher(text).replaceAll(USER_PLACEHOLDER);
    }


    /**
     * Zastępuje liczby placeholderem NUMBER.
     * Konkretne wartości liczbowe rzadko mają znaczenie dla analizy sentymentu.
     */
    private String normalizeNumbers(String text) {
        return numberPattern.matcher(text).replaceAll(NUMBER_PLACEHOLDER);
    }

    /**
     * Przetwarza emotikony zgodnie z specyfiką social media.
     * Emotikony mogą być ważne dla analizy sentymentu.
     */
    private String processEmoticons(String text) {
        // Podstawowe emotikony są zachowywane jako tokeny
        return text;
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

    // ========================================
    // METODY WALIDACJI I POMOCNICZE
    // ========================================

    private boolean isValidInput(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private boolean isValidToken(String token) {
        return !token.isEmpty() && token.length() > minTokenLength;
    }

    private boolean isNotStopWord(String token) {
        return !stopWords.contains(token.toLowerCase());
    }

    private void validateProcessingInput(String text) {
        if (text == null) {
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
