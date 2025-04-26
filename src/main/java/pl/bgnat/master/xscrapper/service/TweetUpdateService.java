package pl.bgnat.master.xscrapper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.dto.Metrics;
import pl.bgnat.master.xscrapper.dto.TweetDto;
import pl.bgnat.master.xscrapper.dto.UserCredential;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.pages.TweetDetailPage;

import java.time.LocalDateTime;
import java.util.Optional;

import static pl.bgnat.master.xscrapper.dto.UserCredential.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TweetUpdateService {

    private final TweetService tweetService;
    private final AdsPowerService adsPowerService;

    @PostConstruct
    public void updateOneTweet() {
        long testId = 27098L;
        updateSingleTweet(testId)
                .ifPresent(tweet -> log.info("Updated tweetId: {}", tweet.id()));
    }

    private Optional<TweetDto> updateSingleTweet(Long tweetId) {
        return tweetService.findTweetById(tweetId)
                .flatMap(this::refreshMetrics)
                .map(tweetService::saveTweet);
    }

    private Optional<Tweet> refreshMetrics(Tweet tweet) {
        // Round-robin pick next user
        User user = User.getNextAvailableUser();
        ChromeDriver driver = adsPowerService.getDriverForUser(user);

        if (driver == null) {
            log.error("Cannot obtain AdsPower browser for user {}", user);
            return Optional.of(tweet);
        }

        try {
            Metrics metrics = scrapeMetrics(tweet.getLink(), driver);
            applyMetrics(tweet, metrics);
            return Optional.of(tweet);
        } catch (Exception e) {
            log.error("Error updating tweet {}: {}", tweet.getId(), e.getMessage(), e);
            return Optional.of(tweet);
        } finally {
            adsPowerService.stopDriver(user);
        }
    }

    private Metrics scrapeMetrics(String url, ChromeDriver driver) {
        var page = new TweetDetailPage(driver, url);
        page.waitForLoad();
        return page.getMetrics();
    }

    private void applyMetrics(Tweet tweet, Metrics m) {
        log.info("Replies: {}, Reposts: {}, Likes: {}, Views: {}", m.getReplies(), m.getReposts(), m.getLikes(), m.getViews());
        tweet.setCommentCount(m.getReplies());
        tweet.setRepostCount(m.getReposts());
        tweet.setLikeCount(m.getLikes());
        tweet.setViews(m.getViews());
        tweet.setUpdateDate(LocalDateTime.now());
    }
}
