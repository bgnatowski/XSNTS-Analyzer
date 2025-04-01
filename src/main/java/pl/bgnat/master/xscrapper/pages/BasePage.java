package pl.bgnat.master.xscrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import pl.bgnat.master.xscrapper.utils.WaitUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.*;

@Slf4j
public abstract class BasePage {
    protected static final String BASE_URL = "https://www.x.com";
    protected WebDriver driver;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.driver.manage().window().maximize();
    }

    protected void open() {
        driver.get(BASE_URL);
    }

    protected void openSubPage(String subUrl) {
        driver.get(BASE_URL + subUrl);
    }

    // Metoda oczekuje na pojedynczy element
    protected WebElement waitForElement(By locator) {
        return WaitUtils.waitForElement(driver, locator);
    }

    // Metoda oczekuje na listę elementów
    protected List<WebElement> waitForElements(By locator) {
        return WaitUtils.waitForElements(driver, locator);
    }

    // Metoda szuka pojedynczego element
    protected WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    // Metoda szuka elementów
    protected List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    // Proste przewinięcie do dołu strony
    protected long scrollToBottom() {
        ((ChromeDriver) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        return calculateHeight();
    }

    protected long scrollBy1000(){
        return scrollBy(1000);
    }

    protected long scrollByRandom(){
        final int minScroll = 200;
        final int maxScroll = 2000;
        int randomScroll = ThreadLocalRandom.current().nextInt(minScroll, maxScroll + 1);
        return scrollBy(randomScroll);
    }

    protected long scrollBy(int x){
        String scrollScript = "window.scrollBy(0, " + x + ")";
        log.info("Scrolluje");
        ((ChromeDriver) driver).executeScript(scrollScript);
        waitRandom();

        return calculateHeight();
    }

    protected void zoomOutAndReturn(){
        driver.get("chrome://settings/");
        executeScript("chrome.settingsPrivate.setDefaultZoom(0.3);");
        driver.navigate().back();
    }

    protected void resetZoomAndReturn(){
        driver.get("chrome://settings/");
        executeScript("chrome.settingsPrivate.setDefaultZoom(1);");
        driver.navigate().back();
    }

    protected void executeScript(String script) {
        ((ChromeDriver) driver).executeScript(script);
    }

    // Odświeżenie strony z krótkim losowym opóźnieniem
    protected void refreshPage() {
        driver.navigate().refresh();
        waitRandom();
    }

    private long calculateHeight(){
        Object expectedHeight = ((ChromeDriver) driver).executeScript("return document.body.scrollHeight");
        long newHeight = 0L;
        if (expectedHeight instanceof Long) {
            newHeight = (long) expectedHeight;
//            log.info("Height: {}", newHeight);
        }
        return newHeight;
    }
}
