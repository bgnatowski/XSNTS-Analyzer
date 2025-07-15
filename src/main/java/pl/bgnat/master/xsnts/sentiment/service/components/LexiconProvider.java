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

@Component
@RequiredArgsConstructor
@Slf4j
public class LexiconProvider {

    @Value("${app.sentiment.lexicon-score-column:3}")
    private int scoreCol;

    @Value("${app.sentiment.lexicon-path:classpath:sentiment/polish_lexicon.tsv}")
    private String lexiconPath;

    private final ResourceLoader loader;
    private Map<String, Double>  lexicon;

    @PostConstruct
    void init() {
        try (var br = new BufferedReader(new InputStreamReader(
                loader.getResource(lexiconPath).getInputStream(), StandardCharsets.UTF_8))) {

            this.lexicon = br.lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("#"))
                    .map(l -> l.split("\\s+"))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    t -> t[0].toLowerCase(),
                                    t -> Double.parseDouble(t[scoreCol]),
                                    Double::min
                            ),
                            Map::copyOf
                    ));

            log.info("Lexicon loaded: {} entries (col {})", lexicon.size(), scoreCol);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load lexicon", e);
        }
    }

    public double score(String tok) {
        return lexicon.getOrDefault(tok.toLowerCase(), 0.0);
    }
}

