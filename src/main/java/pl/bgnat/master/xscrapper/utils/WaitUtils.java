package pl.bgnat.master.xscrapper.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WaitUtils {

    public static WebElement waitForElement(WebDriver driver, By locator) {
        int minWaitSeconds = 3;
        int maxWaitSeconds = 7;

        int randomWaitSeconds = ThreadLocalRandom.current().nextInt(minWaitSeconds, maxWaitSeconds + 1);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(randomWaitSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static List<WebElement> waitForElements(WebDriver driver, By locator) {
        int minWaitSeconds = 3;
        int maxWaitSeconds = 7;

        int randomWaitSeconds = ThreadLocalRandom.current().nextInt(minWaitSeconds, maxWaitSeconds + 1);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(randomWaitSeconds));
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static void waitRandom() {
        try {
            int minWaitSeconds = 2;
            int maxWaitSeconds = 5;
            int randomWaitSeconds = ThreadLocalRandom.current().nextInt(minWaitSeconds, maxWaitSeconds + 1);
            Thread.sleep(randomWaitSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void waitRandom(WebDriver driver, int min, int max) {
        try {
            int randomWaitSeconds = ThreadLocalRandom.current().nextInt(min, max + 1);
            Thread.sleep(randomWaitSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
