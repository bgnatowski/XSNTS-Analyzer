package pl.bgnat.master.xscrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pl.bgnat.master.xscrapper.config.CredentialProperties;
import pl.bgnat.master.xscrapper.model.UserCredential;

import static pl.bgnat.master.xscrapper.model.UserCredential.User;
import static pl.bgnat.master.xscrapper.utils.CookieUtils.loadCookiesFromFile;
import static pl.bgnat.master.xscrapper.utils.CookieUtils.saveCookiesToFile;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Slf4j
public class LoginPage extends BasePage {
    private final CredentialProperties credentialProperties;

    public LoginPage(WebDriver driver, CredentialProperties credentialProperties) {
        super(driver);
        this.credentialProperties = credentialProperties;
    }

    public void loginIfNeeded(User user) {
        open();
        UserCredential userCredential = credentialProperties.getCredentials().get(user.ordinal());
        loadCookiesFromFile(driver, userCredential.cookiePath());

        if (!isLoggedIn()) {
//            log.info("Loguje: {}", user);
            login(userCredential);
//            log.info("Zalogowano: {}", user);
            saveCookiesToFile(driver, userCredential.cookiePath());
            waitRandom();
        }
    }

    private void acceptCookies() {
        By cookiesLocator = By.xpath("//span[text()='Accept all cookies']");
        try {
            WebElement btn = waitForElement(cookiesLocator);
            btn.click();
        } catch (Exception e) {
            // Jeśli przycisk nie zostanie znaleziony, pomijamy - być może już został zaakceptowany
        }
    }

    private void login(UserCredential userCredential) {
        open();
        acceptCookies();
        // Kliknij przycisk logowania
        WebElement loginButton = waitForElement(By.xpath("//a[@data-testid='loginButton']"));
        loginButton.click();
        waitRandom();

        // Wprowadź nazwę użytkownika
        WebElement usernameInput = waitForElement(By.xpath("//input[@name='text']"));
        usernameInput.sendKeys(userCredential.username());
        WebElement nextButton = waitForElement(By.xpath("//span[text()='Dalej']"));
        nextButton.click();
        waitRandom();

        // Opcjonalnie – jeżeli pojawi się formularz email
        try {
            WebElement emailInput = waitForElement(By.xpath("//input[@data-testid='ocfEnterTextTextInput']"));
            emailInput.sendKeys(userCredential.email());
            WebElement emailNextBtn = waitForElement(By.xpath("//span[text()='Dalej']"));
            emailNextBtn.click();
        } catch (Exception e) {
            // formularz email może nie wystąpić – kontynuujemy
        }

        // Wprowadź hasło
        WebElement passwordInput = waitForElement(By.xpath("//input[@name='password']"));
        passwordInput.sendKeys(userCredential.password());
        WebElement loginFormButton = waitForElement(By.xpath("//button[@data-testid='LoginForm_Login_Button']"));
        loginFormButton.click();
        waitRandom();
    }

    private boolean isLoggedIn() {
        try {
            open();
            waitForElement(By.xpath("//article[@data-testid='tweet']"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}