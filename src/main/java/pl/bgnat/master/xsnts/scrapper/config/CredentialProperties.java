package pl.bgnat.master.xsnts.scrapper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import pl.bgnat.master.xsnts.scrapper.dto.UserCredential;
import pl.bgnat.master.xsnts.scrapper.dto.UserProxy;

import java.util.List;

@Data
@ComponentScan
@ConfigurationProperties(prefix = "x")
public class CredentialProperties {
    private List<UserCredential> credentials;

    public String getProxyForUser(UserCredential.User user) {
        UserCredential userCredential = getCredentials().get(user.ordinal());
        String proxyAuth = "";
        if (userCredential.useProxy() && userCredential.proxy() != null) {
            UserProxy proxy = userCredential.proxy();
            proxyAuth = proxy.username() + ":" + proxy.password() + "@" + proxy.host() + ":" + proxy.port();
        }
        return proxyAuth;
    }
}