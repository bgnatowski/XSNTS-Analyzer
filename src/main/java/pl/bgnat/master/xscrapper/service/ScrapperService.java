package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.pages.LoginPage;
import pl.bgnat.master.xscrapper.pages.TrendingPage;
import pl.bgnat.master.xscrapper.pages.WallPage;
import pl.bgnat.master.xscrapper.utils.CookieUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private final TweetService tweetService;
    private final ObjectProvider<ChromeDriver> driverProvider;
    private List<String> currentTrendingKeyword;

    @Value("${x.username}")
    private String username;
    @Value("${x.email}")
    private String email;
    @Value("${x.password}")
    private String password;

//    @Scheduled(cron = "0 0 */2 * * * ")
    public void scheduledScrapeForYouWall() {
        ChromeDriver forYouDriver = driverProvider.getObject();

        LoginPage loginPage = new LoginPage(forYouDriver);
        loginPage.loginIfNeeded(username, email, password);

        WallPage wallPage = new WallPage(forYouDriver);
        wallPage.openForYou();

        Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

        tweetService.saveTweets(scrappedTweets);
        wallPage.exit(); // czy potrzebne
    }

//    @Scheduled(cron = "0 0 */3 * * *")
    @PostConstruct
    public void scheduledScrapeTrendingKeywords() {
        do {
            ChromeDriver trendingDriver = driverProvider.getObject();

            LoginPage loginPage = new LoginPage(trendingDriver);
            loginPage.loginIfNeeded(username, email, password);

            waitRandom();
            TrendingPage trendingPage = new TrendingPage(trendingDriver);
            currentTrendingKeyword = trendingPage.scrapeTrendingKeywords();
            trendingPage.exit();

            waitRandom();
        } while (currentTrendingKeyword.isEmpty());

        scrapeTrendingWall(WallPage.WallType.POPULAR);
        waitRandom();
        scrapeTrendingWall(WallPage.WallType.NEWEST);
    }

    private void scrapeTrendingWall(WallPage.WallType wallType) {
        for (String keyword : currentTrendingKeyword) {
            int trendNr = 1;
            ChromeDriver trendDriver = driverProvider.getObject();
            WallPage wallPage;
            try {
                LoginPage loginPage = new LoginPage(trendDriver);
                loginPage.loginIfNeeded(username, email, password);

                log.info("Otwieram {} trendujacy tag: {}", trendNr, keyword);
                wallPage = new WallPage(trendDriver);

                switch (wallType){
                    case POPULAR -> wallPage.openPopular(keyword);
                    case NEWEST -> wallPage.openNewest(keyword);
                }

                Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

                tweetService.saveTweets(scrappedTweets);
                waitRandom();
            } catch (Exception e) {
                log.error("Błąd przy przetwarzaniu trendu: {}", keyword);
            } finally {
                log.info("Zamykam {} trendujacy tag: {}", trendNr, keyword);
                trendNr++;
            }
        }
    }
}