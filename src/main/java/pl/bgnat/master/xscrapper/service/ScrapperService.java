package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.model.Tweet;

import java.util.List;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitForElements;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private final LoginService loginService;
    private final TweetService tweetService;
    private final ChromeDriver driver;

    @PostConstruct
    public void scrapeOneTweet() {
        loginService.loginToAccount();

        waitRandom();
        List<WebElement> tweets = waitForElements(driver, By.xpath("//article[@data-testid='tweet']"));
        if (tweets.isEmpty()) {
            log.info("Nie znaleziono żadnych tweetów");
            return;
        }
        WebElement firstTweet = tweets.get(0);

        try {
            Tweet tweet = tweetService.parseTweet(firstTweet);
        } catch (NoSuchElementException e) {
            log.info("Nie udało się znaleźć elementów w tweetcie");
        }
    }
}
