package pl.bgnat.master.xsnts.normalization.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Klasa odpowiedzialna za zarządzanie słowami stop.
 * Ładuje słowa stop wyłącznie z pliku.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StopWordsManager {
    private static final Logger log = LoggerFactory.getLogger(StopWordsManager.class);
    private static final String DEFAULT_STOPWORDS_FILE = "/normalization/stopwords_pl.txt";

    public static Set<String> loadStopWords() {
        Set<String> stopWords = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(
                                StopWordsManager.class.getResourceAsStream(DEFAULT_STOPWORDS_FILE)
                        ), StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(stopWords::add);
        } catch (IOException e) {
            log.warn("Błąd podczas ładowania pliku słów stop: {}", DEFAULT_STOPWORDS_FILE, e);
        }

        return Collections.unmodifiableSet(stopWords);
    }
}

