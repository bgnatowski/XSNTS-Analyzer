package pl.bgnat.master.xscrapper.driver;

import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class DriverFactory {
    private final ObjectProvider<ChromeDriver> driverProvider;

    public DriverFactory(ObjectProvider<ChromeDriver> driverProvider) {
        this.driverProvider = driverProvider;
    }

    // Dla przypadku bez proxy
    public ChromeDriver createDriver() {
        return driverProvider.getObject();
    }

    // Dla przypadku z proxy
    public ChromeDriver createDriverWithProxy(String proxyIpPort) {
        if (proxyIpPort == null || proxyIpPort.isEmpty()) {
            return createDriver();
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("disable_infobars");

        Proxy proxy = new Proxy();
        proxy.setHttpProxy(proxyIpPort);
        proxy.setSslProxy(proxyIpPort);
        options.setCapability("proxy", proxy);

        return new ChromeDriver(options);
    }
}