package pl.bgnat.master.xscrapper.pages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.service.TweetService;
import pl.bgnat.master.xscrapper.utils.WaitUtils;

import java.util.ArrayList;
import java.util.List;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.*;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitForElements;

@Slf4j
public class TweetPage extends BasePage {
    private static final int MAX_TWEETS_PER_SCRAPE = 300;
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

    public void scrapeTweets() {
        try {
            int tweetCount = 0;
            int repeatedTweetCount = 0;
            while (true) {
                log.info("Powtorzonych tweetow: {}", repeatedTweetCount);
                waitRandom();
                if (checkForErrorAndStop()) {
                    break;
                }

                log.info("Scrolluje do konca strony");
                scrollToBottom();
                waitRandom();

                List<WebElement> tweetsElements = getTweetElements();
                log.info("Zebrano tweetów: {}", tweetCount);
                if (!tweetsElements.isEmpty()) {
                    List<Tweet> tweetsList = new ArrayList<>();
                    for (WebElement tweetElement : tweetsElements) {
                        Tweet tweet = tweetService.parseTweet(tweetElement);
                        if (StringUtils.hasLength(tweet.getLink())) {
                            if (!tweetService.isExists(tweet)) {
                                tweetsList.add(tweet);
                            } else {
                                repeatedTweetCount++;
                                log.warn("Ponownie ten sam tweet");
                            }
                            tweetCount++;
                        }
                    }
                    tweetService.saveTweets(tweetsList);
                    log.info("Zapisano w podejscu: {}, Ilosc tweetCount: {}", tweetsList.size(), tweetCount);
                    if (repeatedTweetCount >= 50) break;
                    if (tweetCount >= MAX_TWEETS_PER_SCRAPE) {
                        log.info("Osiaganieto limit tweetow. tweetCount: {}", tweetCount);
                        tweetCount = 0;
                        refreshPage();
                    }
                } else {
                    log.info("Nie znaleziono tweetów przy scrollowaniu");
                }
            }
            log.info("Kończę pętlę endless scroll");
        } catch (Exception e) {
            refreshPage();
            log.warn("Wystąpił błąd przy scrapowaniu tweetów; odświeżam stronę.");
            log.error(e.getMessage());
        }
    }
}
