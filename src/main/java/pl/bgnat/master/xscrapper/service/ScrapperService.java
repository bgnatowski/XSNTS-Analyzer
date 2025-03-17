package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.utils.SeleniumHelper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private static final int MAX_TWEETS = 500;
    private final LoginService loginService;
    private final TweetService tweetService;
    private final ChromeDriver driver;

    @PostConstruct
    public void scrapeTweets() {
        loginService.loginToAccount();

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long initialHeight = (long) js.executeScript("return document.body.scrollHeight");

            int tweetCount = 0; // Licznik zapisanych tweetów

            while (tweetCount < MAX_TWEETS) {
                js.executeScript("window.scrollTo(0,document.body.scrollHeight)");
                waitRandom();

                List<WebElement> tweets = waitForElements(driver, By.xpath("//article[@data-testid='tweet']"));
                if (tweets.isEmpty()) {
                    log.info("Nie znaleziono żadnych tweetów");
                    break;
                }

                for (WebElement tweet : tweets) {
                    if (tweetCount >= MAX_TWEETS) break; // Jeśli przekroczymy limit, przerywamy
                    Tweet tweetObj = tweetService.parseTweet(tweet);
                    tweetService.saveTweet(tweetObj);
                    tweetObj = null;
                    tweetCount++;
                }

                if (tweetCount % 100 == 0) {
                    System.gc(); // Wymuszone czyszczenie pamięci
                    log.info("Uruchomiono GC po {} tweetach", tweetCount);
                }

                // Odśwież stronę co X scrolli lub tweetów
                if (tweetCount == MAX_TWEETS) {
                    log.info("Odświeżam stronę...");
                    driver.navigate().refresh();
                    waitRandom(); // Poczekaj chwilę po refreshu
                    tweetService.refreshTweets();
                }

                long currentHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (initialHeight == currentHeight) break; // Jeśli strona się nie przewija, kończymy

                initialHeight = currentHeight;
            }
        } catch (NoSuchElementException | NullPointerException e) {
            log.info("Nie udało się znaleźć elementów w tweetcie");
        }
    }
}
