package pl.bgnat.master.xscrapper.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class TextNormalizer {

    private final Set<String> stopWords;
    private final Pattern urlPattern;
    private final Pattern mentionPattern;
    private final Pattern hashtagPattern;
    private final Pattern numberPattern;
    private final Pattern punctuationPattern;
    private final Pattern whitespacePattern;

    public TextNormalizer() {
        this.stopWords = loadStopWords();
        urlPattern = Pattern.compile("https?://\\S+|www\\.\\S+");
        mentionPattern = Pattern.compile("@\\w+");
        hashtagPattern = Pattern.compile("#\\w+");
        numberPattern = Pattern.compile("\\d+");
        punctuationPattern = Pattern.compile("[^\\p{L}\\p{N}\\s]");
        whitespacePattern = Pattern.compile("\\s+");
    }

    private Set<String> loadStopWords() {
        Set<String> stopWords = new HashSet<>();

        // Podstawowe polskie stop words
        String[] basicStopWords = {
                "i", "w", "z", "na", "do", "o", "że", "się", "nie", "to", "ta", "te",
                "ja", "ty", "on", "ona", "ono", "my", "wy", "oni", "one",
                "jest", "są", "był", "była", "było", "były", "będzie", "będą",
                "a", "ale", "oraz", "lub", "bo", "więc", "jak", "czy", "gdy",
                "dla", "po", "przez", "pod", "nad", "bez", "przy", "od", "za"
        };

        Collections.addAll(stopWords, basicStopWords);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/stopwords_pl.txt"),
                        StandardCharsets.UTF_8))) {

            if (reader != null) {
                reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(stopWords::add);
            }
        } catch (IOException | NullPointerException e) {
            // Jeśli nie ma pliku, używamy podstawowego zestawu
            System.out.println("Używam podstawowego zestawu stop words");
        }

        return stopWords;
    }

    public String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // 1. Usuwanie URL-i
        text = urlPattern.matcher(text).replaceAll(" ");

        // 2. Zastępowanie wzmianek placeholderem
        text = mentionPattern.matcher(text).replaceAll(" USER ");

        // 3. Usuwanie hashtagów ale zostawianie tekstu
        text = hashtagPattern.matcher(text).replaceAll(match ->
                " " + match.group().substring(1) + " ");

        // 4. Zastępowanie liczb placeholderem
        text = numberPattern.matcher(text).replaceAll(" NUMBER ");

        // 5. Konwersja na małe litery
        text = text.toLowerCase();

        // 6. Normalizacja znaków diakrytycznych (opcjonalne - zachowuje polskie znaki)
        text = Normalizer.normalize(text, Normalizer.Form.NFC);

        // 7. Usuwanie interpunkcji
        text = punctuationPattern.matcher(text).replaceAll(" ");

        // 8. Normalizacja białych znaków
        text = whitespacePattern.matcher(text).replaceAll(" ");

        return text.trim();
    }

    public List<String> tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(normalizedText.split("\\s+"))
                .filter(token -> !token.isEmpty())
                .filter(token -> token.length() > 1) // Usuwanie pojedynczych znaków
                .filter(token -> !stopWords.contains(token))
                .collect(Collectors.toList());
    }

    public NormalizedTweet processText(String text) {
        String normalized = normalizeText(text);
        List<String> tokens = tokenize(normalized);

        return NormalizedTweet.builder()
                .normalizedContent(normalized)
                .tokens(tokens)
                .tokenCount(tokens.size())
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class NormalizedTweet {
        private String normalizedContent;
        private List<String> tokens;
        private Integer tokenCount;
    }
}
