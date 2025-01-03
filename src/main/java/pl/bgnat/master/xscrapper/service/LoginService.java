package pl.bgnat.master.xscrapper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.dto.CookieDto;
import pl.bgnat.master.xscrapper.mapper.CookieMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitForElement;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {
    private static final String BASE_URL = "https://www.x.com/";
    private static final String COOKIE_FILE = "cookies.json";
    private final ChromeDriver driver;
    private final ObjectMapper objectMapper;

    @Value("${x.username}")
    private String username;
    @Value("${x.email}")
    private String email;
    @Value("${x.password}")
    private String password;

    public void loginToAccount() {
        loadCookiesFromFile();
        if (!isLoggedIn()) {
            log.info("Nie jesteśmy zalogowani – wykonuję logowanie...");

            driver.get(BASE_URL);
            acceptCookies();
            login();

            waitRandom();
            saveCookiesToFile();
            log.info("Zalogowano.");
        } else {
            log.info("Wygląda na to, że już jesteśmy zalogowani (na podstawie cookies) – pomijam logowanie.");
        }
    }

    private void acceptCookies() {
        try {
            log.info("Before accepting cookies");
            By acceptCookiesButtonLocator = By.xpath("//span[text()='Accept all cookies']");

            WebElement acceptCookiesButton = waitForElement(driver, acceptCookiesButtonLocator);
            acceptCookiesButton.click();
            log.info("After click 'Accept all cookies");

        } catch (TimeoutException | NoSuchElementException e) {
            log.info("Nie znaleziono przycisku 'Accept all cookies' w zadanym czasie. Być może już zaakceptowano lub nie pojawił się. Error message: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Inny błąd podczas klikania 'Accept all cookies'. Error message: {}", e.getMessage());
        }
    }

    private boolean isLoggedIn() {
        try {
            driver.get(BASE_URL);
            WebElement firstTweet = waitForElement(driver, By.xpath("//article[@data-testid='tweet']"));
            if (firstTweet != null) {
                log.info("Znaleziono element tweet w /home – prawdopodobnie jesteśmy zalogowani.");
                return true;
            }
        } catch (TimeoutException e) {
            log.info("Nie znaleziono tweetów w /home. Prawdopodobnie nie zalogowany.");
        }
        return false;
    }

    private void login() {
        try {
            log.info("Before loginButton");
            WebElement loginButton = waitForElement(driver, By.xpath("//a[@data-testid='loginButton']"));
            loginButton.click();
            log.info("After loginButton");

            log.info("Before usernameInput");
            waitRandom();
            WebElement usernameInput = waitForElement(driver, By.xpath("//input[@name='text']"));
            usernameInput.sendKeys(username);
            log.info("After usernameInput");

            log.info("Before nextButton");
            WebElement nextButton= waitForElement(driver, By.xpath("//span[text()='Dalej']"));
            nextButton.click();
            log.info("After nextButton");

            log.info("Before emailInput");
            waitRandom();
            try {
                WebElement mailInput = waitForElement(driver, By.xpath("//input[@data-testid='ocfEnterTextTextInput']"));
                mailInput.sendKeys(email);

                WebElement mailNextButton = waitForElement(driver,By.xpath("//span[text()='Dalej']"));
                mailNextButton.click();
            } catch (TimeoutException e) {
                log.info("Brak formularza nietypowej aktywnosci");
            }
            log.info("After emailInput");

            log.info("Before passwordInput");
            WebElement passwordInput = waitForElement(driver, By.xpath("//input[@name='password']"));
            passwordInput.sendKeys(password);
            log.info("After passwordInput");

            log.info("Before loginFormButton");
            WebElement loginFormButton = waitForElement(driver, By.xpath("//button[@data-testid='LoginForm_Login_Button']"));
            loginFormButton.click();
            log.info("After loginFormButton");

            waitRandom();
            Set<Cookie> cookies = driver.manage().getCookies();
            log.info("Cookies after login = {}", cookies.size());
        } catch (NoSuchElementException e) {
            log.error("Nie znaleziono elementu. Error message: {}", e.getMessage());
        } catch (TimeoutException e) {
            log.info("Timeout Exception occured. Error message: {}", e.getMessage());
        }
    }

    private void saveCookiesToFile() {
        Set<Cookie> cookies = driver.manage().getCookies();

        List<CookieDto> cookieDtoList = cookies.stream()
                .map(CookieMapper.INSTANCE::seleniumCookieToDto)
                .collect(Collectors.toList());

        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(COOKIE_FILE), cookieDtoList);
            log.info("Zapisano {} cookies do pliku {}", cookies.size(), COOKIE_FILE);
        } catch (IOException e) {
            log.error("IOException przy zapisie cookiesow. Message: {}", e.getMessage());
        }
    }

    private void loadCookiesFromFile() {
        File file = new File(COOKIE_FILE);
        if (!file.exists()) {
            log.info("Plik {} nie istnieje. Brak cookies do wczytania.", COOKIE_FILE);
            return;
        }

        try {
            driver.get(BASE_URL);
            List<CookieDto> cookieDtoList = objectMapper.readValue(file, new TypeReference<>() {});

            for (CookieDto dto : cookieDtoList) {
                Cookie cookie = CookieMapper.INSTANCE.buildCookie(dto);
                driver.manage().addCookie(cookie);
            }

            driver.navigate().refresh();
            log.info("Załadowano {} cookies z pliku {}", cookieDtoList.size(), COOKIE_FILE);
        } catch (IOException e) {
            log.error("IOException przy wczytywaniu cookiesow. Message: {}", e.getMessage());
        }
    }
}
