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
    public static final String RETURN_DOCUMENT_BODY_SCROLL_HEIGHT = "return document.body.scrollHeight";
    public static final String ENDLESS_SCROLL_SCRIPT = "window.scrollTo(0,document.body.scrollHeight)";
    private final LoginService loginService;
    private final TweetService tweetService;
    private final ObjectProvider<ChromeDriver> driverProvider;

//    @Scheduled(fixedRate = 3600000)
//    @PostConstruct
    public void scheduledScrapeForYou(){
        ChromeDriver forYouDriver = driverProvider.getObject();
        loginService.loginToAccount(forYouDriver);
        scrapeTweets(forYouDriver);
        forYouDriver.quit();
    }


    //    @Scheduled(fixedRate = 3600000)
    @PostConstruct
    public void scheduledScrapeTrending() {
        ChromeDriver trendingDriver = driverProvider.getObject();

        loginService.loginToAccount(trendingDriver);
        waitRandom();
        trendingDriver.get("https://x.com/explore/tabs/trending");
        waitRandom();
        List<WebElement> trendCells = waitForElements(trendingDriver, By.xpath(".//div[@data-testid='trend' and @role='link']"));
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            WebElement cell = trendCells.get(i);
            try {
                // Z pierwszego dziecka pobieramy wrapper z całą zawartością
                WebElement innerDiv = cell.findElement(By.xpath("./div"));
                // Drugi div wewnątrz wrappera zawiera tekst trendu (np. 'Wrzosek')
                WebElement trendTextElement = innerDiv.findElement(By.xpath("./div[2]//span"));
                String trendKeyword = trendTextElement.getText();

                if (StringUtils.hasLength(trendKeyword)) {
                    executor.submit(() -> {
                        ChromeDriver localDriver = driverProvider.getObject();
                        try {
                            loginService.loginToAccount(localDriver);
                            // Tworzymy adres wyszukiwania z nazwą trendu
                            String searchUrl = "https://x.com/search?q=" +
                                    URLEncoder.encode(trendKeyword, StandardCharsets.UTF_8);
                            localDriver.get(searchUrl);
                            waitRandom();
                            // Uruchamiamy scrapowanie tweetów (endless scroll) na stronie wyszukiwania
                            scrapeTweets(localDriver);
                        } catch (Exception e) {
                            log.error("Błąd przy przetwarzaniu trendu: {}", trendKeyword, e);
                        }
                    });
                }
            } catch (NoSuchElementException e) {
                log.warn("Nie znaleziono elementu trend w elemencie cellInnerDiv", e);
            }
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void scrapeTweets(ChromeDriver driver) {
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

                    if(repetedTweetCount >= 50)
                        break;

                    long currentHeight = (long) driver.executeScript(RETURN_DOCUMENT_BODY_SCROLL_HEIGHT);

                    if (initialHeight == currentHeight || tweetCount >= MAX_TWEETS) {
                        tweetCount = 0;
                        refreshPage(driver);
                    }

                    initialHeight = currentHeight;
                }
                log.info("Nie znaleziono żadnych tweetów");
                refreshPage(driver);
            }
            log.info("Kończę endless scroll w scrapeTweets()");
        } catch (NoSuchElementException | NullPointerException e) {
            log.info("Nie udało się znaleźć elementów w elemencie tweeta");
        }
    }

    private void refreshPage(ChromeDriver driver) {
        log.info("Odświeżam stronę...");
        driver.navigate().refresh();
        waitRandom();
    }
}
