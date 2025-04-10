package pl.bgnat.master.xscrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import pl.bgnat.master.xscrapper.utils.WaitUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Slf4j
public abstract class BasePage {
    protected static final String BASE_URL = "https://www.x.com";
    protected WebDriver driver;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.driver.manage().window().maximize();
    }

    public void exit() {
        driver.quit();
        this.driver = null;
    }

    protected void open() {
        driver.get(BASE_URL);
    }

    protected void openSubPage(String subUrl) {
        driver.get(BASE_URL + subUrl);
    }

    protected WebElement waitForElement(By locator) {
        return WaitUtils.waitForElement(driver, locator);
    }

    protected List<WebElement> waitForElements(By locator) {
        return WaitUtils.waitForElements(driver, locator);
    }

    protected WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    protected List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    public long smartScroll() {
        double randomProbability = Math.random();

        if (randomProbability <= 0.6) { // 60% szans
            log.debug("Wylosowano scrollByRandom (prawdopodobieństwo: {})", randomProbability);
            return scrollByRandom();
        } else {
            log.debug("Wylosowano scrollToBottom (prawdopodobieństwo: {})", randomProbability);
            return scrollToBottom();
        }
    }

    protected long scrollToBottom() {
        ((ChromeDriver) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        return calculateHeight();
    }

    protected long scrollBy1000(){
        return scrollBy(1000);
    }

    protected long scrollByRandom(){
        final int minScroll = 2000;
        final int maxScroll = 10000;
        final int increment = 500;

        int possibleValues = ((maxScroll - minScroll) / increment) + 1;

        int randomIndex = ThreadLocalRandom.current().nextInt(possibleValues);
        int randomScroll = minScroll + (randomIndex * increment);

        return scrollBy(randomScroll);
    }

    protected long scrollBy(int x){
        String scrollScript = "window.scrollBy(0, " + x + ")";
        ((ChromeDriver) driver).executeScript(scrollScript);
        waitRandom();

        return calculateHeight();
    }

    protected void zoomOutAndReturn(){
        try {
            driver.get("chrome://settings/");
            executeScript("chrome.settingsPrivate.setDefaultZoom(0.3);");
            driver.navigate().back();
        } catch (Exception e) {
            log.info("Nie udało się oddalić i wrócić");
        }
    }

    protected void resetZoomAndReturn(){
        try {
            driver.get("chrome://settings/");
            executeScript("chrome.settingsPrivate.setDefaultZoom(1);");
            driver.navigate().back();
        } catch (Exception e) {
            log.info("Nie udało się przywrocić pierwotnego zooma i wrócić");
        }
    }

    protected void executeScript(String script) {
        ((ChromeDriver) driver).executeScript(script);
    }

    protected void refreshPage() {
        driver.navigate().refresh();
    }

    private long calculateHeight(){
        Object expectedHeight = ((ChromeDriver) driver).executeScript("return document.body.scrollHeight");
        long newHeight = 0L;
        if (expectedHeight instanceof Long) {
            newHeight = (long) expectedHeight;
        }
        waitRandom();
        return newHeight;
    }

    protected void navigateRandomly(String keyword) {
        Random random = new Random();
        boolean useSendKeys = random.nextDouble() <= 0.6;
        String searchUrl = "/search?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);

        if (useSendKeys) {
            log.info("Nawigacja za pomocą sendKeys do: {}", keyword);
            try {
                WebElement searchBox = waitForElement(By.xpath("//input[@data-testid='SearchBox_Search_Input']"));
                searchBox.sendKeys(keyword);
                searchBox.sendKeys(Keys.ENTER);

                for (char c : keyword.toCharArray()) {
                    searchBox.sendKeys(String.valueOf(c));
                    waitRandom(50, 150);
                }

                waitRandom(300, 800);
                searchBox.sendKeys(Keys.ENTER);
            } catch (Exception e) {
                log.warn("Błąd przy użyciu sendKeys, przechodzę do metody zapasowej: {}", e.getMessage());
                openSubPage(searchUrl);
            }
        } else {
            log.info("Bezpośrednie otwarcie podstrony: {}", searchUrl);
            openSubPage(searchUrl);
        }

        waitRandom(2000, 5000);
    }

    protected void navigateRandomlyToLatest(String keyword) {
        Random random = new Random();
        boolean useSendKeys = random.nextDouble() <= 0.6;
        String searchUrl = "/search?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8) + "&f=live";

        if (useSendKeys) {
            log.info("Nawigacja za pomocą sendKeys do: {}", keyword);
            try {
                WebElement searchBox = waitForElement(By.xpath("//input[@data-testid='SearchBox_Search_Input']"));
                searchBox.sendKeys(keyword);
                searchBox.sendKeys(Keys.ENTER);

                for (char c : keyword.toCharArray()) {
                    searchBox.sendKeys(String.valueOf(c));
                    waitRandom(50, 150);
                }

                waitRandom(300, 800);
                searchBox.sendKeys(Keys.ENTER);

                waitRandom();
                WebElement newest = waitForElement(By.xpath("//span[contains(text(),'Latest')]"));
                newest.click();
            } catch (Exception e) {
                log.warn("Błąd przy użyciu sendKeys, przechodzę do metody zapasowej: {}", e.getMessage());
                openSubPage(searchUrl);
            }
        } else {
            log.info("Bezpośrednie otwarcie podstrony: {}", searchUrl);
            openSubPage(searchUrl);
        }
        waitRandom(2000, 5000);
    }
}
