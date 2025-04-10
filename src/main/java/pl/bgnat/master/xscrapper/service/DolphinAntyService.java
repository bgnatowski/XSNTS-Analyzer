package pl.bgnat.master.xscrapper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.bgnat.master.xscrapper.config.DolphinProperties;

import java.util.HashMap;
import java.util.Map;

import static pl.bgnat.master.xscrapper.model.UserCredential.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class DolphinAntyService {
    private final DolphinProperties dolphinProperties;
    private final RestTemplate restTemplate;
    private final Map<String, ChromeDriver> runningDrivers = new HashMap<>();

    static {
        // Ustawienie ścieżki do niestandardowego ChromeDrivera
        String os = System.getProperty("os.name").toLowerCase();
        String driverPath;

        if (os.contains("win")) {
            driverPath = "drivers/windows/chromedriver.exe";
        } else if (os.contains("mac")) {
            driverPath = "drivers/mac/chromedriver";
        } else {
            driverPath = "drivers/linux/chromedriver";
        }

        System.setProperty("webdriver.chrome.driver", driverPath);
        log.info("Ustawiono własny ChromeDriver: {}", driverPath);
    }

    public ChromeDriver getDriverForUser(User user) {
        String userName = user.name();

        if (runningDrivers.containsKey(userName)) {
            log.info("Używam istniejącego drivera dla: {}", userName);
            return runningDrivers.get(userName);
        }

        String profileId = dolphinProperties.getProfiles().get(userName);
        if (profileId == null) {
            log.error("Nie znaleziono profilu Dolphin dla użytkownika: {}", userName);
            return null;
        }

        try {
            // Ustal adres endpointu do startu profilu
            String startUrl = dolphinProperties.getUrl() + profileId + "/start?automation=1";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + dolphinProperties.getToken());

            // Wywołaj API Dolphin
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    startUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // Przetwórz odpowiedź
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && (Boolean) responseBody.get("success")) {
                Map<String, Object> automation = (Map<String, Object>) responseBody.get("automation");
                Integer port = (Integer) automation.get("port");

                // Utwórz ChromeDriver podłączony do uruchomionego profilu
                ChromeOptions options = new ChromeOptions();
                options.setExperimentalOption("debuggerAddress", "127.0.0.1:" + port);

                ChromeDriver driver = new ChromeDriver(options);
                runningDrivers.put(userName, driver);
                log.info("Utworzono nowy driver dla: {} na porcie: {}", userName, port);
                return driver;
            } else {
                log.error("Nie udało się uruchomić profilu Dolphin: {}", responseBody);
            }
        } catch (Exception e) {
            log.error("Błąd podczas uruchamiania profilu Dolphin: {}", e.getMessage());
        }

        return null;
    }

    public void stopDriver(User user) {
        String userName = user.name();
        if (!runningDrivers.containsKey(userName)) {
            return;
        }

        try {
            // Zamknij driver
            ChromeDriver driver = runningDrivers.get(userName);
            driver.quit();

            // Wywołaj API do zatrzymania profilu
            String profileId = dolphinProperties.getProfiles().get(userName);
            String stopUrl = dolphinProperties.getUrl() + profileId + "/stop";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + dolphinProperties.getToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(stopUrl, HttpMethod.GET, entity, Map.class);

            runningDrivers.remove(userName);
            log.info("Zatrzymano driver dla: {}", userName);
        } catch (Exception e) {
            log.error("Błąd podczas zatrzymywania profilu: {}", e.getMessage());
        }
    }

}
