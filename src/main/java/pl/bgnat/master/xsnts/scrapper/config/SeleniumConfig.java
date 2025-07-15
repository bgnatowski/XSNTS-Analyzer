package pl.bgnat.master.xsnts.scrapper.config;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class SeleniumConfig {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ChromeDriver driver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable_infobars");
        return new ChromeDriver(options);
    }
}