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
 * Ładuje słowa stop wyłącznie z pliku.
 */
@Slf4j
@Component
public class StopWordsManager {
    private static final String DEFAULT_STOPWORDS_FILE = "/stopwords_pl.txt";

    /**
     * Wczytuje słowa stop z pliku.
     * @return niezmienialny zbiór stopwords po polsku
     */
    public Set<String> loadStopWords() {
        Set<String> stopWords = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream(DEFAULT_STOPWORDS_FILE),
                        StandardCharsets.UTF_8))) {

            if (reader != null) {
                reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(stopWords::add);
            } else {
                log.warn("Nie znaleziono pliku słów stop: {}", DEFAULT_STOPWORDS_FILE);
            }
        } catch (IOException e) {
            log.warn("Błąd podczas ładowania pliku słów stop: {}", DEFAULT_STOPWORDS_FILE, e);
        }

        return Collections.unmodifiableSet(stopWords);
    }
}
