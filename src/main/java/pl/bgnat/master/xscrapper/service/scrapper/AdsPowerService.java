package pl.bgnat.master.xscrapper.service.scrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pl.bgnat.master.xscrapper.config.scrapper.AdsPowerProperties;
import pl.bgnat.master.xscrapper.dto.scrapper.AdsPowerResponse;
import pl.bgnat.master.xscrapper.dto.scrapper.BrowserStatusData;
import pl.bgnat.master.xscrapper.dto.scrapper.WebSocketInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static pl.bgnat.master.xscrapper.dto.scrapper.UserCredential.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdsPowerService {
    private static final String BASE_URL = "http://local.adspower.com:50325/api/v1/browser/";
    private static final int SUCCESS_CODE = 0;
    private static final String BROWSER_ACTIVE_STATUS = "Active";
    private static final String WEBDRIVER_PROPERTY = "webdriver.chrome.driver";

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 sekund

    private final Object browserStartLock = new Object();

    private final AdsPowerProperties properties;
    private final RestTemplate restTemplate;
    private final Map<String, ChromeDriver> runningDrivers = new ConcurrentHashMap<>();

    public ChromeDriver getDriverForUser(User user) {
        return Optional.ofNullable(properties.getUserIds().get(user.name()))
                .map(this::getDriverByIdWithRetry)
                .orElseGet(() -> {
                    log.error("Nie znaleziono ID AdsPower dla użytkownika: {}", user);
                    return null;
                });
    }

    public void stopDriver(User user) {
        Optional.ofNullable(properties.getUserIds().get(user.name()))
                .ifPresent(this::stopDriverById);
    }

    public void stopAllDrivers() {
        new HashSet<>(runningDrivers.keySet())
                .forEach(this::stopDriverById);
    }

    public void refreshActiveBrowsers() {
        try {
            String url = BASE_URL + "local-active";

            Optional.ofNullable(restTemplate.getForEntity(url, Map.class).getBody())
                    .filter(body -> SUCCESS_CODE == (int) body.get("code"))
                    .map(body -> (Map<String, Object>) body.get("data"))
                    .filter(data -> data != null && data.containsKey("list"))
                    .map(data -> (List<Map<String, Object>>) data.get("list"))
                    .ifPresent(this::connectToActiveBrowsers);

        } catch (Exception e) {
            log.error("Błąd podczas odświeżania aktywnych przeglądarek: {}", e.getMessage());
        }
    }

    private ChromeDriver getDriverByIdWithRetry(String userId) {
        ChromeDriver existingDriver = runningDrivers.get(userId);
        if (existingDriver != null) {
            log.info("Używam istniejącego drivera dla ID: {}", userId);
            return existingDriver;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Próba uruchomienia przeglądarki dla ID: {} (próba {}/{})", userId, attempt, MAX_RETRIES);

                synchronized (browserStartLock) {
                    existingDriver = runningDrivers.get(userId);
                    if (existingDriver != null) {
                        log.info("Driver został już utworzony przez inny wątek dla ID: {}", userId);
                        return existingDriver;
                    }

                    Optional<BrowserStatusData> browserStatus = checkBrowserStatus(userId);
                    if (browserStatus.isPresent() && BROWSER_ACTIVE_STATUS.equals(browserStatus.get().status())) {
                        log.info("Znaleziono aktywną przeglądarkę dla ID: {}", userId);
                        return connectToExistingBrowser(userId, browserStatus.get());
                    }

                    ChromeDriver driver = startNewBrowser(userId);
                    if (driver != null) {
                        return driver;
                    }
                }
                if (attempt < MAX_RETRIES) {
                    log.info("Oczekiwanie {} ms przed ponowną próbą dla ID: {}", RETRY_DELAY_MS, userId);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (Exception e) {
                log.error("Błąd podczas uruchamiania przeglądarki dla ID: {} (próba {}/{}): {}",
                        userId, attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        log.info("Oczekiwanie {} ms przed ponowną próbą dla ID: {}", RETRY_DELAY_MS, userId);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }

            }
        }
        log.error("Nie udało się uruchomić przeglądarki po {} próbach dla ID: {}", MAX_RETRIES, userId);
        return null;
    }

    private Optional<BrowserStatusData> checkBrowserStatus(String userId) {
        try {
            String url = BASE_URL + "active?user_id=" + userId;
            ResponseEntity<AdsPowerResponse<BrowserStatusData>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return Optional.ofNullable(response.getBody())
                    .filter(body -> body.code() == SUCCESS_CODE)
                    .map(AdsPowerResponse::data);
        } catch (RestClientException e) {
            log.warn("Nie udało się sprawdzić statusu przeglądarki dla ID: {}", userId, e);
            return Optional.empty();
        }
    }

    private ChromeDriver connectToExistingBrowser(String userId, BrowserStatusData browserData) {
        try {
            String seleniumAddress = browserData.ws().selenium();
            String webdriverPath = browserData.webdriver();

            System.setProperty(WEBDRIVER_PROPERTY, webdriverPath);

            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("debuggerAddress", seleniumAddress);

            ChromeDriver driver = new ChromeDriver(options);
            runningDrivers.put(userId, driver);

            log.info("Połączono z istniejącą przeglądarką dla ID: {}", userId);
            return driver;
        } catch (Exception e) {
            log.error("Błąd podczas łączenia z istniejącą przeglądarką: {}", e.getMessage());
            return null;
        }
    }

    private ChromeDriver startNewBrowser(String userId) {
        try {
            String url = BASE_URL + "start?user_id=" + userId;

            ResponseEntity<AdsPowerResponse<BrowserStatusData>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return Optional.ofNullable(response.getBody())
                    .filter(body -> body.code() == SUCCESS_CODE && body.data() != null)
                    .map(body -> createDriverFromBrowserData(userId, body.data()))
                    .orElseGet(() -> {
                        log.error("Nie udało się uruchomić przeglądarki AdsPower dla usera: {}. Msg: '{}'", userId, response.getBody().msg());
                        return null;
                    });
        } catch (Exception e) {
            log.error("Błąd podczas uruchamiania przeglądarki AdsPower: {}", e.getMessage());
            return null;
        }
    }

    private ChromeDriver createDriverFromBrowserData(String userId, BrowserStatusData data) {
        String chromeDriverPath = data.webdriver();
        String debuggerAddress = data.ws().selenium();

        System.setProperty(WEBDRIVER_PROPERTY, chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", debuggerAddress);

        ChromeDriver driver = new ChromeDriver(options);
        runningDrivers.put(userId, driver);

        log.info("Utworzono nowy driver dla ID: {} z adresem: {}", userId, debuggerAddress);
        return driver;
    }

    private void stopDriverById(String userId) {
        Optional.ofNullable(runningDrivers.get(userId))
                .ifPresent(driver -> {
                    try {
                        closeAllTabsIncludingCurrent(driver);

                        log.info("Zatrzymuje driver dla ID: {}", userId);
                        driver.quit();


                        String url = BASE_URL + "stop?user_id=" + userId;
                        restTemplate.getForObject(url, Object.class);

                        runningDrivers.remove(userId);
                        log.info("Zatrzymano driver");
                    } catch (Exception e) {
                        log.error("Błąd podczas zatrzymywania przeglądarki: {}", e.getMessage());
                    }
                });
    }


    private void connectToActiveBrowsers(List<Map<String, Object>> browsers) {
        browsers.stream()
                .filter(browser -> browser.get("user_id") != null)
                .map(browser -> Map.entry(
                        (String) browser.get("user_id"),
                        createBrowserDataFromMap(browser)
                ))
                .filter(entry -> !runningDrivers.containsKey(entry.getKey()))
                .forEach(entry -> connectToExistingBrowser(entry.getKey(), entry.getValue()));
    }

    private BrowserStatusData createBrowserDataFromMap(Map<String, Object> browser) {
        Map<String, Object> ws = (Map<String, Object>) browser.get("ws");
        String seleniumAddress = (String) ws.get("selenium");
        String webdriverPath = (String) browser.get("webdriver");

        return new BrowserStatusData(
                "", // Status isn't needed for connection
                new WebSocketInfo(seleniumAddress),
                webdriverPath
        );
    }

    private void closeAllTabsIncludingCurrent(ChromeDriver driver) {
        try {
            List<String> windowHandles = new ArrayList<>(driver.getWindowHandles());

            Collections.reverse(windowHandles);

            for (String windowHandle : windowHandles) {
                if (driver.getWindowHandles().contains(windowHandle)) {
                    driver.switchTo().window(windowHandle);
                    driver.close();
                }
            }
            log.info("Zamknięto wszystkie karty przeglądarki");
        } catch (Exception e) {
            log.error("Błąd podczas zamykania wszystkich kart: {}", e.getMessage());
        }
    }


}
