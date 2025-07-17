package pl.bgnat.master.xsnts.normalization.service.processing;

import com.ibm.icu.text.Transliterator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.hasLength;
import static pl.bgnat.master.xsnts.normalization.utils.PatternMatcher.*;

/**
 * Komponent odpowiedzialny za normalizację i tokenizację tekstu tweetów.
 * Implementuje proces wstępnego przetwarzania tekstu zgodnie z praktykami
 * dla analizy treści mediów społecznościowych, w szczególności Twittera/X.
 */
@Slf4j
@Component
@AllArgsConstructor
public class TextNormalizer {
    private static final String USER_PLACEHOLDER = " @ANONYMIZED ";
    private static final String NUMBER_PLACEHOLDER = " ";
    private static final Transliterator UNICODE_NORMALIZER = Transliterator.getInstance("NFKC");

    private final Pattern urlPattern = createUrlPattern();
    private final Pattern mentionPattern = createMentionPattern();
    private final Pattern shortNumberPattern = createShortNumberPattern();
    private final Pattern longNumberPattern = createLongNumberPattern();
    private final Pattern punctuationPattern = createPunctuationPattern();
    private final Pattern whitespacePattern = createWhitespacePattern();
    private final Pattern multiHashPattern  = createMultiHashPattern();
    private final Pattern orphanHashPattern = createOrphanHashPattern();

    /**
     * Wykonuje pełen proces normalizacji i tokenizacji tekstu.
     */
    public String normalize(String text) {
        validateInput(text);

        log.debug("Rozpoczynam pełne przetwarzanie tekstu");

        try {
            return normalizeText(text);

        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania tekstu: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Normalizuje tekst tweeta usuwając szum informacyjny i standaryzując format.
     * Proces normalizacji obejmuje:
     * 1. Usuwanie URL-i
     * 2. Normalizację wzmianek (@username) - anonimizacja lub usuwanie
     * 3. Zamianę liczb na placeholdery (zostawiamy daty)
     * 4. Normalizację znaków Unicode 𝕍𝕆𝕋𝔼  Łódź  →  VOTE Łódź
     * 5. Konwersję na małe litery
     * 6. Usuwanie interpunkcji
     * 7. Korekta hasztagow
     * 8. Normalizację białych znaków
     *
     * @param text tekst do normalizacji
     * @return znormalizowany tekst
     */
    private String normalizeText(String text) {
        if (!hasLength(text)) {
            log.debug("Otrzymano pusty lub null tekst do normalizacji");
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

        // Krok 4: Normalizacja Unicode
        normalized = normalizeUnicode(normalized);

        // Krok 5: Konwersja na małe litery
        normalized = normalized.toLowerCase();

        // Krok 6: Usunięcie interpunkcji
        normalized = removePunctuation(normalized);

        // Krok 7: Korekta hashtagow:
        normalized = normalizeHashtags(normalized);

        // Krok 8: Normalizacja białych znaków (wielokrotne spacje)
        normalized = normalizeWhitespace(normalized);

        log.debug("Zakończono normalizację. Długość po przetworzeniu: {}", normalized.length());
        return normalized.trim();
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
        String normalizedShort = shortNumberPattern.matcher(text).replaceAll(NUMBER_PLACEHOLDER);
        return longNumberPattern.matcher(normalizedShort).replaceAll(NUMBER_PLACEHOLDER);
    }

    /**
     * Normalizuje znaki Unicode zachowując polskie znaki diakrytyczne.
     */
    private String normalizeUnicode(String text) {
        return UNICODE_NORMALIZER.transliterate(text);
    }

    /**
     * Usuwa interpunkcję zachowując litery, cyfry i spacje.
     */
    private String removePunctuation(String text) {
        return punctuationPattern.matcher(text).replaceAll(" ");
    }

    /**
     * 1. Sprowadza "##hasztag", "###hasztag" … do "#hasztag"
     * 2. Usuwa samotne # lub ich wielokrotność.
     */
    private String normalizeHashtags(String text) {
        // krok 1 – redukcja wielu # do jednego
        String collapsed = multiHashPattern.matcher(text).replaceAll("#");
        // krok 2 – usunięcie osieroconych #
        return orphanHashPattern.matcher(collapsed).replaceAll(" ");
    }

    /**
     * Normalizuje białe znaki zastępując wielokrotne spacje pojedynczą spacją.
     */
    private String normalizeWhitespace(String text) {
        return whitespacePattern.matcher(text).replaceAll(" ");
    }


    private void validateInput(String text) {
        if (!hasLength(text)) {
            throw new IllegalArgumentException("Tekst do przetworzenia nie może być null");
        }
    }

}
