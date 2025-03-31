package pl.bgnat.master.xscrapper.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pl.bgnat.master.xscrapper.utils.CookieUtils;
import pl.bgnat.master.xscrapper.utils.WaitUtils;

import static pl.bgnat.master.xscrapper.utils.CookieUtils.loadCookiesFromFile;
import static pl.bgnat.master.xscrapper.utils.CookieUtils.saveCookiesToFile;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.*;

public class LoginPage extends BasePage {
    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void acceptCookies() {
        By cookiesLocator = By.xpath("//span[text()='Accept all cookies']");
        try {
            WebElement btn = waitForElement(cookiesLocator);
            btn.click();
        } catch (Exception e) {
            // Jeśli przycisk nie zostanie znaleziony, pomijamy - być może już został zaakceptowany
        }
    }

    public void login(String username, String email, String password) {
        open();
        acceptCookies();
        // Kliknij przycisk logowania
        WebElement loginButton = waitForElement(By.xpath("//a[@data-testid='loginButton']"));
        loginButton.click();
        waitRandom();

        // Wprowadź nazwę użytkownika
        WebElement usernameInput = waitForElement(By.xpath("//input[@name='text']"));
        usernameInput.sendKeys(username);
        WebElement nextButton = waitForElement(By.xpath("//span[text()='Dalej']"));
        nextButton.click();
        waitRandom();

        // Opcjonalnie – jeżeli pojawi się formularz email
        try {
            WebElement emailInput = waitForElement(By.xpath("//input[@data-testid='ocfEnterTextTextInput']"));
            emailInput.sendKeys(email);
            WebElement emailNextBtn = waitForElement(By.xpath("//span[text()='Dalej']"));
            emailNextBtn.click();
        } catch (Exception e) {
            // formularz email może nie wystąpić – kontynuujemy
        }
        waitRandom();

        // Wprowadź hasło
        WebElement passwordInput = waitForElement(By.xpath("//input[@name='password']"));
        passwordInput.sendKeys(password);
        WebElement loginFormButton = waitForElement(By.xpath("//button[@data-testid='LoginForm_Login_Button']"));
        loginFormButton.click();
        waitRandom();

        saveCookiesToFile(driver);
    }

    public boolean isLoggedIn() {
        try {
            open();
            waitForElement(By.xpath("//article[@data-testid='tweet']"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void loginIfNeeded(String username, String email, String password) {
        open();
        loadCookiesFromFile(driver);
//        refreshPage();

        if (!isLoggedIn()) {
            login(username, email, password);
        }
    }
}