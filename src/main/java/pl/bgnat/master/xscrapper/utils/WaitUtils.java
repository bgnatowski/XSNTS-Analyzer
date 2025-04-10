package pl.bgnat.master.xscrapper.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WaitUtils {
    private static final int MIN_WAIT_MILLIS = 3000;
    private static final int MAX_WAIT_MILLIS = 10000;

    public static WebElement waitForElement(WebDriver driver, By locator) {
        int randomWaitSeconds = ThreadLocalRandom.current().nextInt(MIN_WAIT_MILLIS, MAX_WAIT_MILLIS + 1);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(randomWaitSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static List<WebElement> waitForElements(WebDriver driver, By locator) {
        int randomWaitSeconds = ThreadLocalRandom.current().nextInt(MIN_WAIT_MILLIS, MAX_WAIT_MILLIS + 1);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(randomWaitSeconds));
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static void waitRandom() {
        waitRandom(MIN_WAIT_MILLIS, MAX_WAIT_MILLIS);
    }

    public static void waitRandom(int minMs, int maxMs) {
        try {
            int randomWaitSeconds = ThreadLocalRandom.current().nextInt(minMs, maxMs + 1);
            Thread.sleep(randomWaitSeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
