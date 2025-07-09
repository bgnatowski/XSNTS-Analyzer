package pl.bgnat.master.xscrapper.config.sentiment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.sentiment")
@Data
public class SentimentProperties {
    private String lexiconPath;
    private double positiveThreshold;
    private double negativeThreshold;
}
