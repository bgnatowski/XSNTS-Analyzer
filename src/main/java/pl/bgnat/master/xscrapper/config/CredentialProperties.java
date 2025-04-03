package pl.bgnat.master.xscrapper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import pl.bgnat.master.xscrapper.model.UserCredential;

import java.util.List;

@Data
@ComponentScan
@ConfigurationProperties(prefix = "x")
public class CredentialProperties {
    private List<UserCredential> credentials;
}