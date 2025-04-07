package pl.bgnat.master.xscrapper.driver;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

import static pl.bgnat.master.xscrapper.utils.FileUtils.*;

@Component
public class DriverFactory {
    private final ObjectProvider<ChromeDriver> driverProvider;
    private static final String EXTENSION_DIR = "chrome_proxy_ext";

    public DriverFactory(ObjectProvider<ChromeDriver> driverProvider) {
        this.driverProvider = driverProvider;
    }

    private ChromeDriver createDriver() {
        return driverProvider.getObject();
    }

    public ChromeDriver createDriverWithAuthProxy(String proxyAuth) {
        if (proxyAuth == null || proxyAuth.isEmpty()) {
            return createDriver();
        }

        String[] parts = proxyAuth.split("@");
        String[] credentials = parts[0].split(":");
        String[] address = parts[1].split(":");

        String username = credentials[0];
        String password = credentials[1];
        String proxyHost = address[0];
        String proxyPort = address[1];


        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-infobars", "--no-sandbox");
//        options.addArguments("--disable-web-security");
//        options.addArguments("--disable-blink-features=WebRTC");
//        options.addArguments("--disable-site-isolation-trials");
//        options.addArguments("--disable-webrtc-hw-decoding");
//        options.addArguments("--disable-webrtc-hw-encoding");
//        options.addArguments("--disable-webrtc-multiple-routes");
//        options.addArguments("--disable-webrtc-hw-crypto");
//        options.addArguments("--disable-webrtc-dtls-crypto");


        File extensionDir = createProxyExtension(proxyHost, proxyPort, username, password);
        if (extensionDir != null) {
            options.addArguments("--load-extension=" + extensionDir.getAbsolutePath());
        }

        return new ChromeDriver(options);
    }

    private File createProxyExtension(String host, String port, String username, String password) {
        try {
            File extensionDir = new File(EXTENSION_DIR);
            if (!extensionDir.exists() && !extensionDir.mkdirs()) {
                throw new IOException("Nie można utworzyć katalogu: " + EXTENSION_DIR);
            }

            copyResourceFile("proxy-extension/manifest.json", new File(extensionDir, "manifest.json"));

            String backgroundTemplate = readResourceFile("proxy-extension/background.js.template");
            String backgroundContent = backgroundTemplate
                    .replace("${proxyHost}", host)
                    .replace("${proxyPort}", port)
                    .replace("${proxyUsername}", username)
                    .replace("${proxyPassword}", password);

            writeFile(new File(extensionDir, "background.js"), backgroundContent);

            return extensionDir;
        } catch (IOException e) {
            System.err.println("Błąd podczas tworzenia rozszerzenia proxy: " + e.getMessage());
            return null;
        }
    }
}
