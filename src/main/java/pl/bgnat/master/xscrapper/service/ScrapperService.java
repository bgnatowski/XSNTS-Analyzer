package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.pages.LoginPage;
import pl.bgnat.master.xscrapper.pages.TrendingPage;
import pl.bgnat.master.xscrapper.pages.WallPage;

import java.util.List;
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
    @PostConstruct
    public void scheduledScrapeForYouWall() {
        ChromeDriver forYouDriver = driverProvider.getObject();

        LoginPage loginPage = new LoginPage(forYouDriver);
        loginPage.loginIfNeeded(username, email, password);

        WallPage wallPage = new WallPage(forYouDriver);
        wallPage.openForYou();

        Set<WebElement> scrappedTweets = wallPage.scrapeTweets();
        forYouDriver.quit(); //?

        tweetService.saveTweets(scrappedTweets);
//        forYouDriver.quit(); // czy tu?
    }

    //    @Scheduled(cron = "0 0 */3 *  * *")
    public void scheduledScrapeTrendingWall() {
        ChromeDriver loginDriver = driverProvider.getObject();
        LoginPage loginPage = new LoginPage(loginDriver);
        loginPage.loginIfNeeded(username, email, password);

//        for (String keyword : currentTrendingKeyword) {
//            int trendNr = 1;
//            try {
//                ChromeDriver localDriver = driverProvider.getObject();
//                try {
//                    String searchUrl = "https://x.com/search?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
//                    CookieUtils.copyCookies(loginDriver, localDriver, searchUrl);
//
//                    log.info("Otwieram {} trendujacy tag: {}, link: {}", finalI + 1, trendKeyword, searchUrl + newest);
//                    localDriver.get(searchUrl + newest);
//
//                    waitRandom();
//                    scrapeTweets(localDriver);
//                } catch (Exception e) {
//                    log.error("Błąd przy przetwarzaniu trendu: {}", trendKeyword);
//                } finally {
//                    log.info("Zamykam {} trendujacy tag: ", finalI + 1, trendKeyword);
//                    localDriver.quit();
//                }
//            } catch (NoSuchElementException e) {
//                log.warn("Nie znaleziono elementu trend w elemencie cellInnerDiv", e);
//            }
//        }
    }

    //    @Scheduled(cron = "0 0 * * * *")
    public void scheduledScrapeTrendingNewestWall() {
//        String newest = isNewest ? "&f=live" : "";
    }

    @Scheduled(cron = "0 0 */3 * * *")
    private void scheduledScrapeTrendingKeywords() {
        ChromeDriver trendingDriver = driverProvider.getObject();

        LoginPage loginPage = new LoginPage(trendingDriver);
        loginPage.loginIfNeeded(username, email, password);

        waitRandom();
        TrendingPage trendingPage = new TrendingPage(trendingDriver);
        currentTrendingKeyword = trendingPage.scrapeTrendingKeywords();

        waitRandom();
        trendingDriver.quit();
    }
}