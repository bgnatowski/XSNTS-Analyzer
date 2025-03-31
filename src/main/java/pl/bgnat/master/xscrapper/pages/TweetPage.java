package pl.bgnat.master.xscrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitForElements;

@Slf4j
public class TweetPage extends BasePage{
    public TweetPage(WebDriver driver) {
        super(driver);
    }

    // Pobiera listę tweetów na stronie – zakładamy, że każdy tweet jest reprezentowany przez article z data-testid="tweet"
    public List<WebElement> getTweetElements() {
        return waitForElements(By.xpath("//article[@data-testid='tweet']"));
    }

    public boolean checkForErrorAndStop() {
        try {
            By errorLocator = By.xpath("//span[text()='Something went wrong. Try reloading.']");

            findElement(errorLocator);
            log.info("Blokada. Wystapilo: 'Something went wrong. Try reloading.'");
            return true;
        } catch (NoSuchElementException elementException) {
            log.info("Brak zawieszenia z 'Something went wrong. Try reloading.'");
            return false;
        }
    }
}
