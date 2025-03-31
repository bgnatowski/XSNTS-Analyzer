package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.pages.LoginPage;
import pl.bgnat.master.xscrapper.pages.TrendingPage;
import pl.bgnat.master.xscrapper.pages.TweetPage;
import pl.bgnat.master.xscrapper.utils.WaitUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private static final int MAX_TWEETS = 300;
    private final TweetService tweetService;
    private final ObjectProvider<ChromeDriver> driverProvider;

    @Value("${x.username}")
    private String username;
    @Value("${x.email}")
    private String email;
    @Value("${x.password}")
    private String password;

//    @Scheduled(cron = "0 */2 * * * *")
//    @PostConstruct
    public void scheduledScrapeForYou() {
        ChromeDriver forYouDriver = driverProvider.getObject();
        try {
            LoginPage loginPage = new LoginPage(forYouDriver);
            loginPage.loginIfNeeded(username, email, password);
            TweetPage tweetPage = new TweetPage(forYouDriver);
            scrapeTweets(tweetPage);
        } finally {
            forYouDriver.quit();
        }
    }

//    @Scheduled(cron = "0 */3 * * * *")
    @PostConstruct
    public void scheduledScrapeTrendingNormal(){
        scheduledScrapeTrending(false);
    }

//    @Scheduled(cron = "0 0 * * * *")
    public void scheduledScrapeTrendingNewest(){
        scheduledScrapeTrending(true);
    }

    private void scheduledScrapeTrending(boolean isNewest) {
        ChromeDriver trendingDriver = driverProvider.getObject();

        String newest = isNewest ? "&f=live" : "";

        LoginPage loginPage = new LoginPage(trendingDriver);
        loginPage.loginIfNeeded(username, email, password);

        waitRandom();
        TrendingPage trendingPage = new TrendingPage(trendingDriver);
        trendingPage.openTrending();

        long lastHeight = 0L;
        String collectedTrends;

        Set<String> trendsKeywordsSet = new LinkedHashSet<>();
        while (true){
            log.info("Pobieram trendy (widoczne)");
            List<WebElement> trendElements = trendingPage.getTrendElements();
            log.info("Pobrano: {}", trendElements.size());

            trendElements.forEach(webElement -> {
                String keywordNr = trendingPage.extractTrending(webElement);
//                log.info(keywordNr);
                trendsKeywordsSet.add(keywordNr);
            });

            waitRandom();
            log.info("Scrolluje");
            long newHeight = trendingPage.scrollBy1000();

            if(newHeight == lastHeight && trendsKeywordsSet.size() == 30){
                collectedTrends = String.join(" ", trendsKeywordsSet);
                log.info("30 aktualnych trendow: {}", collectedTrends);
                break;
            } else {
                log.info("Ponawiam pętle");
                lastHeight = newHeight;
            }
        }

        List<String> keywords = trendingPage.extractTrendKeyword(trendsKeywordsSet);
        keywords.forEach(System.out::println);

//        for (int i = 0; i < 10; i++) {
//            WebElement cell = trendCells.get(i);
//            try {
//                WebElement innerDiv = cell.findElement(By.xpath("./div"));
//                WebElement trendTextElement = innerDiv.findElement(By.xpath("./div[2]//span"));
//                String trendKeyword = trendTextElement.getText();
//
//                if (StringUtils.hasLength(trendKeyword)) {
//                    int finalI = i;
//                        ChromeDriver localDriver = driverProvider.getObject();
//                        try {
//                            loginService.copyCookies(trendingDriver, localDriver);
//
//                            String searchUrl = "https://x.com/search?q=" + URLEncoder.encode(trendKeyword, StandardCharsets.UTF_8);
//                            log.info("Otwieram {} trendujacy tag: {}, link: {}", finalI+1, trendKeyword, searchUrl+newest);
//                            localDriver.get(searchUrl+newest);
//
//                            waitRandom();
//                            scrapeTweets(localDriver);
//                        } catch (Exception e) {
//                            log.error("Błąd przy przetwarzaniu trendu: {}", trendKeyword);
//                        } finally {
//                            log.info("Zamykam {} trendujacy tag: ", finalI + 1, trendKeyword);
//                            localDriver.quit();
//                        }
//                }
//            } catch (NoSuchElementException e) {
//                log.warn("Nie znaleziono elementu trend w elemencie cellInnerDiv", e);
//            }
//        }

        log.info("Zamykam trendingDriver");
        trendingDriver.quit();
    }

    private void scrapeTweets(TweetPage tweetPage) {
        try {
            int tweetCount = 0;
            int repeatedTweetCount = 0;
            while (true) {
                log.info("Powtorzonych tweetow: {}", repeatedTweetCount);
                WaitUtils.waitRandom();
                if (tweetPage.checkForErrorAndStop()) {
                    break;
                }

                log.info("Scrolluje do konca strony");
                tweetPage.scrollToBottom();
                WaitUtils.waitRandom();

                List<WebElement> tweetsElements = tweetPage.getTweetElements();
                log.info("Zebrano tweetów: {}", tweetCount);
                if (!tweetsElements.isEmpty()) {
                    List<Tweet> tweetsList = new ArrayList<>();
                    for (WebElement tweetElement : tweetsElements) {
                        Tweet tweet = tweetService.parseTweet(tweetElement);
                        if (StringUtils.hasLength(tweet.getLink())) {
                            if (!tweetService.isExists(tweet)) {
                                tweetsList.add(tweet);
                            } else {
                                repeatedTweetCount++;
                                log.warn("Ponownie ten sam tweet");
                            }
                            tweetCount++;
                        }
                    }
                    tweetService.saveTweets(tweetsList);
                    log.info("Zapisano w podejscu: {}, Ilosc tweetCount: {}", tweetsList.size(), tweetCount);
                    if (repeatedTweetCount >= 50) break;
                    if (tweetCount >= MAX_TWEETS) {
                        log.info("Osiaganieto limit tweetow. tweetCount: {}", tweetCount);
                        tweetCount = 0;
                        tweetPage.refreshPage();
                    }
                } else {
                    log.info("Nie znaleziono tweetów przy scrollowaniu");
                }
            }
            log.info("Kończę pętlę endless scroll");
        } catch (Exception e) {
            tweetPage.refreshPage();
            log.warn("Wystąpił błąd przy scrapowaniu tweetów; odświeżam stronę.");
            log.error(e.getMessage());
        }
    }
}