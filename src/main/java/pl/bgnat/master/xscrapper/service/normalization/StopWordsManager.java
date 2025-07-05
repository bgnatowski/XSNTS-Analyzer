package pl.bgnat.master.xscrapper.service.normalization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Klasa odpowiedzialna za zarządzanie słowami stop.
 */
@Slf4j
@Component
public class StopWordsManager {
    private static final String DEFAULT_STOPWORDS_FILE = "/stopwords_pl.txt";

    public Set<String> loadStopWords() {
        Set<String> stopWords = new HashSet<>();

        // Załadowanie podstawowych słów stop
        loadBasicStopWords(stopWords);

        // Próba załadowania z pliku
        loadFromFile(stopWords);

        return stopWords;
    }

    private void loadBasicStopWords(Set<String> stopWords) {
        String[] basicStopWords = {
                // Zaimki
                "ja", "ty", "on", "ona", "ono", "my", "wy", "oni", "one",
                // Przyimki
                "w", "z", "na", "do", "o", "przez", "pod", "nad", "bez", "przy", "od", "za",
                // Spójniki
                "i", "a", "ale", "oraz", "lub", "bo", "więc", "że",
                // Czasowniki pomocnicze
                "jest", "są", "był", "była", "było", "były", "będzie", "będą",
                // Przysłówki
                "nie", "się", "jak", "czy", "gdy", "dla", "po",
                "user", "tak", "ten", "tym", "tego", "już", "tylko", "może", "być",
                "kto", "też", "jego", "która", "które", "lat", "roku", "dzień"
        };

        Collections.addAll(stopWords, basicStopWords);
    }

    private void loadFromFile(Set<String> stopWords) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream(DEFAULT_STOPWORDS_FILE),
                        StandardCharsets.UTF_8))) {

            if (reader != null) {
                reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(stopWords::add);
            }
        } catch (IOException | NullPointerException e) {
            log.warn("Nie można załadować pliku słów stop: {}. Używam podstawowego zestawu",
                    DEFAULT_STOPWORDS_FILE);
        }
    }
}