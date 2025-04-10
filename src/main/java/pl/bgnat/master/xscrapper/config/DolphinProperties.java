package pl.bgnat.master.xscrapper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "dolphin")
@Data
public class DolphinProperties {
    private String token;
    private String url;
    private Map<String, String> profiles = new HashMap<>();
}

