package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.config.CredentialProperties;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.pages.LoginPage;
import pl.bgnat.master.xscrapper.pages.TrendingPage;
import pl.bgnat.master.xscrapper.pages.WallPage;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static pl.bgnat.master.xscrapper.model.UserCredential.User.*;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private final TweetService tweetService;
    private final ObjectProvider<ChromeDriver> driverProvider;
    private final CredentialProperties credentialProperties;

    private List<String> currentTrendingKeyword;

    //    @Scheduled(cron = "0 0 */2 * * * ")
    @PostConstruct
    public void scheduledScrapeForYouWall() {
        ChromeDriver forYouDriver = driverProvider.getObject();
        LoginPage loginPage = new LoginPage(forYouDriver, credentialProperties);
        loginPage.loginIfNeeded(USER_2);

        WallPage wallPage = new WallPage(forYouDriver);
        wallPage.openForYou();

        Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

        tweetService.saveTweets(scrappedTweets);
    }

    //    @Scheduled(cron = "0 0 */4 * * *")
//    @PostConstruct
    public void scheduledScrapeTrendingKeywords() {
        do {
            ChromeDriver trendingDriver = driverProvider.getObject();

            LoginPage loginPage = new LoginPage(trendingDriver, credentialProperties);
            loginPage.loginIfNeeded(USER_1);

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
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (String keyword : currentTrendingKeyword) {
            executor.submit(() -> {
                String originalName = Thread.currentThread().getName();
                Thread.currentThread().setName(originalName + "-" + keyword);
                ChromeDriver trendDriver = driverProvider.getObject();
                WallPage wallPage;
                try {
                    LoginPage loginPage = new LoginPage(trendDriver, credentialProperties);
                    loginPage.loginIfNeeded(USER_1);

                    wallPage = new WallPage(trendDriver);

                    switch (wallType) {
                        case POPULAR -> wallPage.openPopular(keyword);
                        case NEWEST -> wallPage.openNewest(keyword);
                    }

                    Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

                    tweetService.saveTweets(scrappedTweets);
                    waitRandom();

                    wallPage.exit();
                    log.info("Zamykam trendujacy tag: {}", keyword);
                } catch (Exception e) {
                    log.error("Błąd przy przetwarzaniu trendu: {}", keyword);
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(4, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}