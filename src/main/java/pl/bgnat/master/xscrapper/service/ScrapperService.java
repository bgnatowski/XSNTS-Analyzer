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
    private static final int MAX_TWEETS = 300;
    public static final String ENDLESS_SCROLL_SCRIPT = "window.scrollTo(0,document.body.scrollHeight)";
    private final LoginService loginService;
    private final TweetService tweetService;
    private final ObjectProvider<ChromeDriver> driverProvider;

    @Scheduled(cron = "0 */2 * * * *")
    public void scheduledScrapeForYou() {
        ChromeDriver forYouDriver = driverProvider.getObject();
        loginService.loginToAccount(forYouDriver);
        scrapeTweets(forYouDriver);
        forYouDriver.quit();
    }

    @Scheduled(cron = "0 */3 * * * *")
    public void scheduledScrapeTrendingNormal(){
        scheduledScrapeTrending(false);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledScrapeTrendingNewest(){
        scheduledScrapeTrending(true);
    }

    public void scheduledScrapeTrending(boolean isNewest) {
        ChromeDriver trendingDriver = driverProvider.getObject();

        String newest = isNewest ? "&f=live" : "";

        loginService.loginToAccount(trendingDriver);
        waitRandom(trendingDriver, 2, 10);
        trendingDriver.get("https://x.com/explore/tabs/trending");
        waitRandom(trendingDriver, 1, 5);

        List<WebElement> trendCells = waitForElements(trendingDriver,
                By.xpath(".//div[@data-testid='trend' and @role='link']"));

        for (int i = 0; i < 10; i++) {
            WebElement cell = trendCells.get(i);
            try {
                WebElement innerDiv = cell.findElement(By.xpath("./div"));
                WebElement trendTextElement = innerDiv.findElement(By.xpath("./div[2]//span"));
                String trendKeyword = trendTextElement.getText();

                if (StringUtils.hasLength(trendKeyword)) {
                    int finalI = i;
                        ChromeDriver localDriver = driverProvider.getObject();
                        try {
                            loginService.copyCookies(trendingDriver, localDriver);

                            String searchUrl = "https://x.com/search?q=" + URLEncoder.encode(trendKeyword, StandardCharsets.UTF_8);
                            log.info("Otwieram {} trendujacy tag: {}, link: {}", finalI+1, trendKeyword, searchUrl+newest);
                            localDriver.get(searchUrl+newest);

                            waitRandom(localDriver, 2, 10);
                            scrapeTweets(localDriver);
                        } catch (Exception e) {
                            log.error("Błąd przy przetwarzaniu trendu: {}", trendKeyword);
                        } finally {
                            log.info("Zamykam {} trendujacy tag: ", finalI + 1, trendKeyword);
                            localDriver.quit();
                        }
                }
            } catch (NoSuchElementException e) {
                log.warn("Nie znaleziono elementu trend w elemencie cellInnerDiv", e);
            }
        }

        log.info("Zamykam trendingDriver");
        trendingDriver.quit();
    }

    public void scrapeTweets(ChromeDriver driver) {
        try {
            int tweetCount = 0;
            int repetedTweetCount = 0;

            while (true) {
                waitRandom(driver, 2, 10);
                driver.executeScript(ENDLESS_SCROLL_SCRIPT);
                waitRandom(driver, 2, 10);

                if (tweetService.checkForErrorAndStop(driver)) {
                    log.info("Zatrzymuję działanie z powodu blokady.");
                    break;
                }

                List<WebElement> tweetsElements = waitForElements(driver, By.xpath("//article[@data-testid='tweet']"));
                log.info("Zebrano tweetów: {}", tweetCount);
                if (!tweetsElements.isEmpty()) {
                    List<Tweet> tweetsList = new ArrayList<>();
                    for (WebElement tweetElement : tweetsElements) {
                        Tweet tweet = tweetService.parseTweet(tweetElement);
                        if (StringUtils.hasLength(tweet.getLink())) {
                            if (!tweetService.isExists(tweet)) {
                                tweetsList.add(tweet);
                                log.info("Dodano tweeta do listy.");
                            } else {
                                repetedTweetCount++;
                                log.warn("Ponownie ten sam tweet");
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
                    waitRandom(driver, 2, 10);
                }
                log.info("Nie znaleziono tweetow przy scrollu");
            }
            log.info("Kończę petle endless scroll");
        } catch (NoSuchElementException | NullPointerException e) {
            log.warn("RefreshPage - wystapil blad");
        }
    }

    private void refreshPage(ChromeDriver driver) {
        log.info("Odświeżam stronę...");
        driver.navigate().refresh();
        waitRandom(driver, 2, 10);
    }
}
