package pl.bgnat.master.xscrapper.config.sentiment;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import pl.bgnat.master.xscrapper.service.sentiment.LexiconLoader;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SentimentProperties.class)
public class SentimentModuleConfig {

    private final SentimentProperties sentimentProperties;
    private final LexiconLoader loader;

    @jakarta.annotation.PostConstruct
    void loadLexicon() {
        loader.load(sentimentProperties.getLexiconPath());
    }
}
