package pl.bgnat.master.xscrapper.service.sentiment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
@Getter
public class LexiconLoader {

    private final ResourceLoader rl;
    private final Map<String, Double> lexicon = new ConcurrentHashMap<>();

    public void load(String locations) {
        Arrays.stream(locations.split(","))
                .map(String::trim)
                .forEach(this::importSingle);
        log.info("📚 Załadowano łącznie {} haseł (po scaleniu duplikatów)", lexicon.size());
    }

    /* ---------- prywatne ---------- */

    private void importSingle(String path) {
        try (var reader = new BufferedReader(new InputStreamReader(
                rl.getResource(path).getInputStream(), StandardCharsets.UTF_8))) {

            reader.lines()
                    .filter(l -> !l.isBlank() && l.charAt(0) != '#')
                    .forEach(this::parseLine);

            log.info("  • {} wczytano OK", path);
        } catch (Exception e) {
            throw new IllegalStateException("Błąd wczytywania: " + path, e);
        }
    }

    private void parseLine(String line) {
        String[] p = line.split("[\t;]");
        if (p.length < 2) return;

        String lemma = p[0].trim().toLowerCase();
        double val   = Double.parseDouble(p[1].replace(',', '.'));
        lexicon.merge(lemma, val, (oldVal, newVal) -> (oldVal + newVal) / 2);
    }


    /** Używane przez SentimentAnalyzer */
    public double polarity(String lemma) {
        return lexicon.getOrDefault(lemma, 0.0);
    }
}

