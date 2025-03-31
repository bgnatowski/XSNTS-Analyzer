package pl.bgnat.master.xscrapper.pages;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import pl.bgnat.master.xscrapper.utils.WaitUtils;

import java.util.List;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.*;

@AllArgsConstructor
@RequiredArgsConstructor
@Slf4j
public abstract class BasePage {
    protected static final String BASE_URL = "https://www.x.com/";
    protected WebDriver driver;

    protected void open() {
        driver.get(BASE_URL);
    }

    protected void openSubPage(String subUrl) {
        driver.get(BASE_URL + subUrl);
    }

    // Metoda oczekuje na pojedynczy element
    public WebElement waitForElement(By locator) {
        return WaitUtils.waitForElement(driver, locator);
    }

    // Metoda oczekuje na listę elementów
    public List<WebElement> waitForElements(By locator) {
        return WaitUtils.waitForElements(driver, locator);
    }

    // Metoda oczekuje na pojedynczy element
    public WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    // Metoda oczekuje na listę elementów
    public List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    // Proste przewinięcie do dołu strony
    public void scrollToBottom() {
        ((ChromeDriver) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    public long scrollBy1000(){
        ((ChromeDriver) driver).executeScript("window.scrollBy(0, 1000)");
        waitRandom();

        Object expectedHeight = ((ChromeDriver) driver).executeScript("return document.body.scrollHeight");
        long newHeight = 0L;
        if (expectedHeight instanceof Long) {
             newHeight = (long) expectedHeight;
        }
        log.info("newHeight {}", newHeight);

        return newHeight;
    }

    // Odświeżenie strony z krótkim losowym opóźnieniem
    public void refreshPage() {
        driver.navigate().refresh();
        waitRandom();
    }
}
