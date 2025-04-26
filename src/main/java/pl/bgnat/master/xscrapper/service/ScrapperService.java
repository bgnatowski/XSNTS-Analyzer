package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.config.CredentialProperties;
import pl.bgnat.master.xscrapper.dto.UserCredential;
import pl.bgnat.master.xscrapper.dto.UserCredential.User;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.pages.LoginPage;
import pl.bgnat.master.xscrapper.pages.WallPage;
import pl.bgnat.master.xscrapper.pages.WallPage.WallType;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static pl.bgnat.master.xscrapper.dto.UserCredential.User.USER_2;
import static pl.bgnat.master.xscrapper.pages.WallPage.WallType.LATEST;
import static pl.bgnat.master.xscrapper.pages.WallPage.WallType.POPULAR;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private final TweetService tweetService;
    private final AdsPowerService adsPowerService;
    private final CredentialProperties credentialProperties;
    private final Trends24ScraperService trends24ScraperService;

    private List<String> currentTrendingKeyword = new ArrayList<>();

    public void scheduledScrapeForYouWallAsync() {
        int credentialCount = 5;
        int index = 0;

        ExecutorService executor = Executors.newFixedThreadPool(credentialCount);
        for (User user : User.values()) {
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

//    @Scheduled(cron = "0 0 */3 * * *")
//    @PostConstruct
    public void scheduledScrapePopularKeywords() {
        do {
            updateTrendingKeywords();
        } while (currentTrendingKeyword.isEmpty());

        scrapeTrendingWall(POPULAR);
        waitRandom();
    }

//    @Scheduled(cron = "0 0 */4 * * *")
    public void scheduledScrapeLatestKeywords() {
        do {
            updateTrendingKeywords();
        } while (currentTrendingKeyword.isEmpty());

        scrapeTrendingWall(LATEST);
        waitRandom();
    }

//    @PostConstruct
    private void scrapeOneByKeyword() {
        String keyword = "konklawa";
        WallType wallType = LATEST;
        User user = USER_2;
        WallPage wallPage;
        ChromeDriver trendDriver = null;
        try {
            trendDriver = adsPowerService.getDriverForUser(user);

            LoginPage loginPage = new LoginPage(trendDriver, credentialProperties);
            loginPage.loginIfNeeded(user);

            wallPage = new WallPage(trendDriver, user);

            switch (wallType) {
                case POPULAR -> wallPage.openPopular(keyword);
                case LATEST -> wallPage.openLatest(keyword);
            }

            Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

            tweetService.saveTweets(scrappedTweets);
            waitRandom();
        } catch (Exception e) {
            log.error("Błąd przy przetwarzaniu keyworda: {}", keyword);
        } finally {
            try {
                if (trendDriver != null) {
                    adsPowerService.stopDriver(user);
                }
            } catch (Exception e) {
                log.error("Błąd podczas zatrzymywania przeglądarki dla użytkownika: {}: {}",
                        user, e.getMessage());
            }
        }
    }

    private void scrapeForYou(User user) {
        ChromeDriver userDriver = adsPowerService.getDriverForUser(user);

        LoginPage loginPage = new LoginPage(userDriver, credentialProperties);
        loginPage.loginIfNeeded(user);

        WallPage wallPage = new WallPage(userDriver, user);
        wallPage.openForYou();

        Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

        tweetService.saveTweets(scrappedTweets);

        log.info("Zamykam ForYou dla: {}", user);
        wallPage.exit();
    }

    private void scrapeTrendingWall(WallType wallType) {
        int credentialCount = 5;

        Queue<String> keywordsQueue = new LinkedList<>(currentTrendingKeyword);

        ExecutorService executor = Executors.newFixedThreadPool(credentialCount);

        AtomicInteger counter = new AtomicInteger(0);

        while (!keywordsQueue.isEmpty()) {
            String keyword = keywordsQueue.poll();
            int currentCounter = counter.incrementAndGet();

            executor.submit(() -> {
                String originalName = Thread.currentThread().getName();

                int userIndex = (currentCounter - 1) % credentialCount;
                User user = UserCredential.getUser(userIndex);

                ChromeDriver trendDriver = null;

                try {
                    String formattedThreadName = getFormattedThreadName(keyword, userIndex, currentCounter);
                    Thread.currentThread().setName(formattedThreadName);

                    log.info("Rozpoczynam przetwarzanie trendu: {} dla użytkownika: {}", keyword, user);

                    trendDriver = adsPowerService.getDriverForUser(user);

                    if (trendDriver == null) {
                        log.error("Nie udało się uzyskać przeglądarki dla użytkownika: {} i trendu: {}", user, keyword);
                        return;
                    }

                    LoginPage loginPage = new LoginPage(trendDriver, credentialProperties);
                    loginPage.loginIfNeeded(user);

                    WallPage wallPage = new WallPage(trendDriver, user);

                    switch (wallType) {
                        case POPULAR -> wallPage.openPopular(keyword);
                        case LATEST -> wallPage.openLatest(keyword);
                    }

                    Set<Tweet> scrappedTweets = wallPage.scrapeTweets();
                    tweetService.saveTweets(scrappedTweets);

                    log.info("Zakończono przetwarzanie trendu: {} dla użytkownika: {}", keyword, user);
                } catch (Exception e) {
                    log.error("Błąd przy przetwarzaniu trendu: {} dla użytkownika: {}. ErrorMsg: {}",
                            keyword, user, e.getMessage());
                } finally {
                    try {
                        if (trendDriver != null) {
                            adsPowerService.stopDriver(user);
                        }
                    } catch (Exception e) {
                        log.error("Błąd podczas zatrzymywania przeglądarki dla użytkownika: {}: {}",
                                user, e.getMessage());
                    }

                    Thread.currentThread().setName(originalName);
                    log.info("Zamykam trendujacy tag: {}", keyword);
                }
            });

            // Dodaj małe opóźnienie między zadaniami, aby zapobiec przeciążeniu API AdsPower
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
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

    private void updateTrendingKeywords() {
        try {
            List<String> uniqueTrends = trends24ScraperService.scrapeUniqueTrendingKeywords();

            if (!uniqueTrends.isEmpty()) {
                currentTrendingKeyword.clear();
                currentTrendingKeyword.addAll(uniqueTrends);

                if (currentTrendingKeyword.size() > 50) {
                    currentTrendingKeyword = currentTrendingKeyword.subList(0, 50);
                }

                log.info("Zaktualizowano listę trendujących słów kluczowych: {}", currentTrendingKeyword);
            } else {
                log.warn("Nie znaleziono nowych unikalnych trendów");
            }
        } catch (Exception e) {
            log.error("Błąd podczas aktualizacji trendujących słów kluczowych: {}", e.getMessage(), e);
        }
    }
}