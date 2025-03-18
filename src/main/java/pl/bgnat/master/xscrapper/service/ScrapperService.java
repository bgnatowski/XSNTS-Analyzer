package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.model.Tweet;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public static final String ENDLESS_SCROLL_SCRIPT = "window.scrollTo(0,document.body.scrollHeight)";
    private final LoginService loginService;
    private final TweetService tweetService;
    private final ObjectProvider<ChromeDriver> driverProvider;

    //    @Scheduled(fixedRate = 3600000)
//    @PostConstruct
    public void scheduledScrapeForYou() {
        ChromeDriver forYouDriver = driverProvider.getObject();
        loginService.loginToAccount(forYouDriver);
        scrapeTweets(forYouDriver);
        forYouDriver.quit();
    }


    //    @Scheduled(fixedRate = 3600000)
    @PostConstruct
    public void scheduledScrapeTrending() {
        //Tworzymy główną instancję sterownika i logujemy się –sesja jest utrzymywana
        ChromeDriver trendingDriver = driverProvider.getObject();

        loginService.loginToAccount(trendingDriver);
        waitRandom();
        trendingDriver.get("https://x.com/explore/tabs/trending");
        waitRandom();
        // Pobieramy elementy trendów – selektor odpowiada divowi z atrybutami data-testid='trend' i role='link'
        List<WebElement> trendCells = waitForElements(trendingDriver,
                By.xpath(".//div[@data-testid='trend' and @role='link']"));

        int trendsToProcess = Math.min(5, trendCells.size());
        for (int i = 0; i < 3; i++) {
            WebElement cell = trendCells.get(i);
            try {
                WebElement innerDiv = cell.findElement(By.xpath("./div"));
                WebElement trendTextElement = innerDiv.findElement(By.xpath("./div[2]//span"));
                String trendKeyword = trendTextElement.getText();
                if (StringUtils.hasLength(trendKeyword)) {
                    // Tworzymy nową instancję sterownika – zamiast logowania, kopiujemy sesję (cookies)
                    ChromeDriver localDriver = driverProvider.getObject();
                    try {
                        // Skopiuj ciasteczka (sesję) z trendingDriver do localDriver
                        loginService.copyCookies(trendingDriver, localDriver);

                        // Tworzymy adres wyszukiwania z nazwą trendu
                        String searchUrl = "https://x.com/search?q=" +
                                URLEncoder.encode(trendKeyword, StandardCharsets.UTF_8);
                        localDriver.get(searchUrl);
                        waitRandom();
                        // Scrapujemy tweety na stronie wyszukiwania
                        scrapeTweets(localDriver);
                    } catch (Exception e) {
                        log.error("Błąd przy przetwarzaniu trendu: {}", trendKeyword, e);
                    } finally {
                        localDriver.quit();
                    }
                }
            } catch (NoSuchElementException e) {
                log.warn("Nie znaleziono elementu trend w elemencie cellInnerDiv", e);
            }
        }
    }

    public void scrapeTweets(ChromeDriver driver) {
        try {
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
                        if (StringUtils.hasLength(tweet.getLink())) {
                            if (!tweetService.isExists(tweet)) {
                                tweetsList.add(tweet);
                            } else {
                                log.warn("Ponownie ten sam tweet");
                                repetedTweetCount++;
                            }
                            tweetCount++;
                        }
                    }

                    tweetService.saveTweets(tweetsList);

                    if (repetedTweetCount >= 50)
                        break;

                    if (tweetCount >= MAX_TWEETS) {
                        tweetCount = 0;
                        refreshPage(driver);
                    }
                }
                log.info("Nie znaleziono żadnych tweetów");
            }
            log.info("Kończę endless scroll w scrapeTweets()");
        } catch (NoSuchElementException | NullPointerException e) {
            refreshPage(driver);
            log.info("Nie udało się znaleźć elementów w elemencie tweeta");
        }
    }

    private void refreshPage(ChromeDriver driver) {
        log.info("Odświeżam stronę...");
        driver.navigate().refresh();
        waitRandom();
    }
}
