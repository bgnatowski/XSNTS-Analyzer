package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.config.CredentialProperties;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.model.UserCredential;
import pl.bgnat.master.xscrapper.model.UserCredential.User;
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
//    @PostConstruct
    public void scheduledScrapeForYouWall() {
        ChromeDriver forYouDriver = driverProvider.getObject();
        LoginPage loginPage = new LoginPage(forYouDriver, credentialProperties);
        loginPage.loginIfNeeded(USER_2);

        WallPage wallPage = new WallPage(forYouDriver);
        wallPage.openForYou();

        Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

        tweetService.saveTweets(scrappedTweets);
    }

    //    @Scheduled(cron = "0 0 */6 * * *")
    @PostConstruct
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

        scrapeTrendingWall(WallPage.WallType.NEWEST);
        waitRandom();
    }

    private void scrapeTrendingWall(WallPage.WallType wallType) {
        int credentialCount = UserCredential.SIZE;
        int index = 0;

        ExecutorService executor = Executors.newFixedThreadPool(credentialCount);
        for (String keyword : currentTrendingKeyword) {
            final int userIndex = index % credentialCount;
            final int keywordIndex = index+1;
            index++;

            executor.submit(() -> {
                String originalName = Thread.currentThread().getName();

                String formattedThreadName = getFormattedThreadName(keyword, userIndex, keywordIndex);
                Thread.currentThread().setName(formattedThreadName);

                ChromeDriver trendDriver = driverProvider.getObject();
                WallPage wallPage;
                try {
                    User userToBeLoggedIn = UserCredential.getUser(userIndex);
                    LoginPage loginPage = new LoginPage(trendDriver, credentialProperties);
                    loginPage.loginIfNeeded(userToBeLoggedIn);

                    String usedUsername = credentialProperties.getCredentials().get(userIndex).username();
                    log.info("Dla keyword: {} używam credentials użytkownika: {}", keyword, usedUsername);

                    wallPage = new WallPage(trendDriver);

                    switch (wallType) {
                        case POPULAR -> wallPage.openPopular(keyword);
                        case NEWEST -> wallPage.openNewest(keyword);
                    }

                    Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

                    tweetService.saveTweets(scrappedTweets);
                    waitRandom();

                    log.info("Zamykam trendujacy tag: {}", keyword);
                    wallPage.exit();
                } catch (Exception e) {
                    log.error("Błąd przy przetwarzaniu trendu: {}", keyword);
                } finally {
                    Thread.currentThread().setName(originalName);
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

    private static String getFormattedThreadName(String keyword, int userIndex, int keywordIndex) {
        String leftPart = String.format("t%d%02d", userIndex, keywordIndex);
        int maxKeywordLength = 15 - (leftPart.length()+1);
        String truncatedKeyword = (keyword.length() > maxKeywordLength)
                ? keyword.substring(0, maxKeywordLength)
                : keyword;
        return String.format("%s-%s", leftPart, truncatedKeyword);
    }
}