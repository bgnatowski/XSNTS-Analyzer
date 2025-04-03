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
    private static final int MIN_WAIT_SECONDS = 3;
    private static final int MAX_WAIT_SECONDS = 10;

    // Oczekuje na pojawienie się pojedynczego elementu
    public static WebElement waitForElement(WebDriver driver, By locator) {
        int randomWaitSeconds = ThreadLocalRandom.current().nextInt(MIN_WAIT_SECONDS, MAX_WAIT_SECONDS + 1);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(randomWaitSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // Oczekuje na pojawienie się listy elementów
    public static List<WebElement> waitForElements(WebDriver driver, By locator) {
        int randomWaitSeconds = ThreadLocalRandom.current().nextInt(MIN_WAIT_SECONDS, MAX_WAIT_SECONDS + 1);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(randomWaitSeconds));
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static void waitRandom() {
        waitRandom(MIN_WAIT_SECONDS, MAX_WAIT_SECONDS);
    }

    public static void waitRandom(int minSeconds, int maxSeconds) {
        try {
            int randomWaitSeconds = ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds + 1);
            Thread.sleep(randomWaitSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
