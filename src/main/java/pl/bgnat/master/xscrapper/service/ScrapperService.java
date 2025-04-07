package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.config.CredentialProperties;
import pl.bgnat.master.xscrapper.driver.DriverFactory;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.model.UserCredential;
import pl.bgnat.master.xscrapper.model.UserCredential.User;
import pl.bgnat.master.xscrapper.pages.LoginPage;
import pl.bgnat.master.xscrapper.pages.TrendingPage;
import pl.bgnat.master.xscrapper.pages.WallPage;
import pl.bgnat.master.xscrapper.pages.WallPage.WallType;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static pl.bgnat.master.xscrapper.model.UserCredential.User.USER_1;
import static pl.bgnat.master.xscrapper.model.UserCredential.User.USER_3;
import static pl.bgnat.master.xscrapper.pages.WallPage.WallType.*;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private final TweetService tweetService;
    private final DriverFactory driverFactory;
    private final CredentialProperties credentialProperties;

    private List<String> currentTrendingKeyword;

    //    @Scheduled(cron = "0 0 */2 * * * ")
//    @PostConstruct
    public void scheduledScrapeForYouWall() {
        scrapeForYou(USER_3);
    }

    //    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledScrapeForYouWallAsync() {
        int credentialCount = UserCredential.SIZE;
        int index = 0;

        ExecutorService executor = Executors.newFixedThreadPool(credentialCount);
        for (User user : UserCredential.User.values()) {
            final int userIndex = index % credentialCount;
            final int keywordIndex = index + 1;
            index++;

            executor.submit(() -> {
                String originalName = Thread.currentThread().getName();
                try {
                    String formattedThreadName = getFormattedThreadName(user.name(), userIndex, keywordIndex);
                    Thread.currentThread().setName(formattedThreadName);

                    scrapeForYou(user);
                    waitRandom();
                } catch (Exception e) {
                    log.error("Błąd przy przetwarzaniu ForYou dla: {}", user.name());
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

    private void scrapeForYou(User user) {
        String proxyForUser = credentialProperties.getProxyForUser(user);
        ChromeDriver userDriver = driverFactory.createDriverWithAuthProxy(proxyForUser);

        LoginPage loginPage = new LoginPage(userDriver, credentialProperties);
        loginPage.loginIfNeeded(user);

        WallPage wallPage = new WallPage(userDriver);
        wallPage.openForYou();

        Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

        tweetService.saveTweets(scrappedTweets);

        log.info("Zamykam ForYou dla: {}", user);
        wallPage.exit();
    }

    @PostConstruct
    public void scheduledScrapeTrendingKeywords() {
        do {
            String proxyIpPort = credentialProperties.getProxyForUser(USER_1);
            ChromeDriver trendingDriver = driverFactory.createDriverWithAuthProxy(proxyIpPort);

            LoginPage loginPage = new LoginPage(trendingDriver, credentialProperties);
            loginPage.loginIfNeeded(USER_1);

            waitRandom();
            TrendingPage trendingPage = new TrendingPage(trendingDriver);
            currentTrendingKeyword = trendingPage.scrapeTrendingKeywords();
            trendingPage.exit();

            waitRandom();
        } while (currentTrendingKeyword.isEmpty());

        scrapeTrendingWall(LATEST);
        waitRandom();
    }

    private void scrapeTrendingWall(WallType wallType) {
        int credentialCount = UserCredential.SIZE;
        int index = 0;

        ExecutorService executor = Executors.newFixedThreadPool(credentialCount);
        for (String keyword : currentTrendingKeyword) {
            final int userIndex = index % credentialCount;
            final int keywordIndex = index + 1;
            index++;

            executor.submit(() -> {
                String originalName = Thread.currentThread().getName();
                try {
                    String formattedThreadName = getFormattedThreadName(keyword, userIndex, keywordIndex);
                    Thread.currentThread().setName(formattedThreadName);

                    User user = UserCredential.getUser(userIndex);
                    String proxyForUser = credentialProperties.getProxyForUser(user);
                    ChromeDriver trendDriver = driverFactory.createDriverWithAuthProxy(proxyForUser);

                    LoginPage loginPage = new LoginPage(trendDriver, credentialProperties);
                    loginPage.loginIfNeeded(user);

                    String usedUsername = credentialProperties.getCredentials().get(userIndex).username();
                    log.info("Dla keyword: {} używam credentials użytkownika: {}", keyword, usedUsername);

                    WallPage wallPage = new WallPage(trendDriver);

                    switch (wallType) {
                        case POPULAR -> wallPage.openPopular(keyword);
                        case LATEST -> wallPage.openLatest(keyword);
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
        int maxKeywordLength = 15 - (leftPart.length() + 1);
        String truncatedKeyword = (keyword.length() > maxKeywordLength)
                ? keyword.substring(0, maxKeywordLength)
                : keyword;
        return String.format("%s-%s", leftPart, truncatedKeyword);
    }
}