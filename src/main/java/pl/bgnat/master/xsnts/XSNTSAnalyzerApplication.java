package pl.bgnat.master.xsnts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import pl.bgnat.master.xsnts.scrapper.config.CredentialProperties;
import pl.bgnat.master.xsnts.sentiment.config.SentimentProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({CredentialProperties.class, SentimentProperties.class})
public class XSNTSAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(XSNTSAnalyzerApplication.class, args);
    }
}
