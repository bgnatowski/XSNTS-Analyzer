package pl.bgnat.master.xscrapper.config.scrapper;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "adspower")
@Data
public class AdsPowerProperties {
    private Map<String, String> userIds = new HashMap<>();
}
