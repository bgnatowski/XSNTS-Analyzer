package pl.bgnat.master.xscrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.*;

@Slf4j
public class WallPage extends BasePage {
    private static final int MAX_TWEETS_PER_SCRAPE = 300;

    public WallPage(WebDriver driver) {
        super(driver);
    }

    public List<WebElement> scrapeTweets() {
        List<WebElement> tweetsElements = new ArrayList<>();
        while (true) {
            try {
                waitRandom();
                if (checkForErrorAndStop()) {
                    break;
                }

                waitRandom();
                log.info("Scrolluje");
                scrollBy1000();

                List<WebElement> scrappedTweetElements = getTweetElements();
                log.info("Zebrano tweetow przy scrollu: {}", scrappedTweetElements.size());
                if (!scrappedTweetElements.isEmpty()) {
                    tweetsElements.addAll(scrappedTweetElements);
                    if (tweetsElements.size() >= MAX_TWEETS_PER_SCRAPE) {
                        log.info("Osiaganieto limit tweetElementow. Przerywam petle.");
                        break;
                    }
                } else {
                    log.info("Nie znaleziono tweetów przy scrollowaniu. Ponawiam petle.");
                }
            } catch (Exception e) {
                refreshPage();
                log.warn("Wystąpił błąd przy scrapowaniu tweetów; Odświeżam stronę.");
            }
        }
        log.info("Kończę pętlę endless scroll");
        return tweetsElements;
    }

    // Pobiera listę tweetów na stronie – zakładamy, że każdy tweet jest reprezentowany przez article z data-testid="tweet"
    private List<WebElement> getTweetElements() {
        return waitForElements(By.xpath("//article[@data-testid='tweet']"));
    }

    private boolean checkForErrorAndStop() {
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
