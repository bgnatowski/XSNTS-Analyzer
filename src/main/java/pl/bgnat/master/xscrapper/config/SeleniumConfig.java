package pl.bgnat.master.xscrapper.config;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeleniumConfig {
    @Bean
    public ChromeDriver driver() {
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
        return new ChromeDriver(options);
    }
}