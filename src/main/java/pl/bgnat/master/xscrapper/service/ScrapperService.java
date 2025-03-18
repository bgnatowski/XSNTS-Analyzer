package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.model.Tweet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitForElements;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private static final int MAX_TWEETS = 200;
    public static final String RETURN_DOCUMENT_BODY_SCROLL_HEIGHT = "return document.body.scrollHeight";
    public static final String ENDLESS_SCROLL_SCRIPT = "window.scrollTo(0,document.body.scrollHeight)";
    private final LoginService loginService;
    private final TweetService tweetService;
    private final ChromeDriver driver;

//    @Scheduled(fixedRate = 3600000)
    @PostConstruct
    public void scheduledScrapeForYou(){
        loginService.loginToAccount();
        scrapeTweets();
    }




    public void scrapeTweets() {
        try {
            long initialHeight = (long) driver.executeScript(RETURN_DOCUMENT_BODY_SCROLL_HEIGHT);
            int tweetCount = 0;
            int repetedTweetCount = 0;

            while (true) {
                driver.executeScript(ENDLESS_SCROLL_SCRIPT);
                waitRandom();

                List<WebElement> tweetsElements = waitForElements(driver, By.xpath("//article[@data-testid='tweet']"));

                if (!tweetsElements.isEmpty()) {
                    List<Tweet> tweetsList = new ArrayList<>();
                    for (WebElement tweetElement : tweetsElements) {
                        Tweet tweet = tweetService.parseTweet(tweetElement);
                        if(StringUtils.hasLength(tweet.getLink())){
                            if(!tweetService.isExists(tweet)) {
                                tweetsList.add(tweet);
                            } else {
                                log.warn("Ponownie ten sam tweet");
                                repetedTweetCount++;
                            }
                            tweetCount++;
                        }
                    }

                    tweetService.saveTweets(tweetsList);

                    if(repetedTweetCount >= 5)
                        break;

                    long currentHeight = (long) driver.executeScript(RETURN_DOCUMENT_BODY_SCROLL_HEIGHT);

                    if (initialHeight == currentHeight || tweetCount >= MAX_TWEETS) {
                        tweetCount = 0;
                        refreshPage();
                    }

                    initialHeight = currentHeight;
                }
                log.info("Nie znaleziono żadnych tweetów");
            }
            log.info("Kończę endless scroll w scrapeTweets()");
        } catch (NoSuchElementException | NullPointerException e) {
            log.info("Nie udało się znaleźć elementów w elemencie tweeta");
        }
    }

    private void refreshPage() {
        log.info("Odświeżam stronę...");
        driver.navigate().refresh();
        waitRandom();
    }
}
