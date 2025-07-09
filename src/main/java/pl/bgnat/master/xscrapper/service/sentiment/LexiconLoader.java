package pl.bgnat.master.xscrapper.service.sentiment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Getter
public class LexiconLoader {

    private final ResourceLoader resourceLoader;
    private final Map<String, Double> lexicon = new HashMap<>();

    public void load(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            try (BufferedReader br = new BufferedReader(
                    new java.io.InputStreamReader(
                            resource.getInputStream(), StandardCharsets.UTF_8))) {

                br.lines()
                        .filter(line -> !line.startsWith("#") && !line.isBlank())
                        .forEach(line -> {
                            String[] parts = line.split("\t");
                            if (parts.length >= 2) {
                                lexicon.put(parts[0], Double.parseDouble(parts[1]));
                            }
                        });
                log.info("Załadowano {} haseł leksykonu sentymentu", lexicon.size());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Nie można wczytać leksykonu: " + path, e);
        }
    }
}
