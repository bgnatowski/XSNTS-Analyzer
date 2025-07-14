package pl.bgnat.master.xsnts.sentiment.service.components;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LexiconProvider {

    private final ResourceLoader loader;

    @Value("${app.sentiment.lexicon-path:classpath:sentiment/polish_lexicon.tsv}")
    private String lexiconPath;

    private Map<String, Double> lexicon;

    @PostConstruct
    void init() {
        try {
            Resource resource = loader.getResource(lexiconPath);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                this.lexicon = reader.lines()
                        .filter(l -> !l.isBlank() && !l.startsWith("#"))
                        .map(l -> l.split("\\s+"))
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        t -> t[0].toLowerCase(),                // klucz
                                        t -> Double.parseDouble(t[1]),          // wartość
                                        Double::min                                     // merge ⇒ mniejsza wartość
                                ),
                                Map::copyOf));

                log.info("Lexicon loaded – {} entries", lexicon.size());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load sentiment lexicon", e);
        }
    }

    public double score(String token) {
        return lexicon.getOrDefault(token.toLowerCase(), 0.0);
    }
}
