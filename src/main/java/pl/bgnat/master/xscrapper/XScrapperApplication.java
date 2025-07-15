package pl.bgnat.master.xscrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import pl.bgnat.master.xscrapper.config.scrapper.CredentialProperties;
import pl.bgnat.master.xscrapper.config.sentiment.SentimentProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({CredentialProperties.class, SentimentProperties.class})
public class XScrapperApplication {
    public static void main(String[] args) {
        SpringApplication.run(XScrapperApplication.class, args);
    }
}
