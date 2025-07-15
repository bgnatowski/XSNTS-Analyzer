package pl.bgnat.master.xscrapper.service.scrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xscrapper.dto.topicmodeling.Metrics;
import pl.bgnat.master.xscrapper.model.scrapper.Tweet;
import pl.bgnat.master.xscrapper.pages.TweetDetailPage;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static pl.bgnat.master.xscrapper.dto.scrapper.UserCredential.User;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class TweetUpdateService {
    private static final int NUM_BROWSERS      = 5;
    private static final int BATCH_PER_BROWSER = 25;

    private final TweetService tweetService;
    private final AdsPowerService adsPowerService;
    private final ExecutorService executor = Executors.newFixedThreadPool(NUM_BROWSERS);

    public void bulkRefreshTweets(List<Long> tweetIds) {
        BlockingQueue<Long> queue = new LinkedBlockingQueue<>(tweetIds);
        Set<Tweet> updatedTweets = new HashSet<>();

        for (int i = 0; i < NUM_BROWSERS; i++) {
            executor.submit(() -> {
                var user   = User.getNextAvailableUser();
                var driver = adsPowerService.getDriverForUser(user);
                int count  = 0;

                try {
                    while (true) {
                        Long id = queue.poll(1, TimeUnit.SECONDS);
                        if (id == null) break;

                        log.info("Refreshuje tweeta o id: {}", id);
                        updatedTweets.add(updateOne(id, driver));
                        count++;

                        log.info("Count: {}", count);
                        if (count % BATCH_PER_BROWSER == 0) {
                            log.info("Osiagnieto count: {}. Restartuje przegladarke", count);
                            adsPowerService.stopDriver(user);
                            waitRandom();
                            driver = adsPowerService.getDriverForUser(user);
                            if (driver == null) {
                                log.error("Restart przegladarki dla {} niepowodzenie.", user);
                                break;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    log.info("Zaktualizowano tweety. Zapisuje w db.");
                    tweetService.updateTweets(updatedTweets);
                    adsPowerService.stopDriver(user);
                }
            });
        }
    }

    @Transactional
    public Tweet updateOne(Long tweetId, ChromeDriver driver) {
        Tweet tweetToUpdate = tweetService.findTweetById(tweetId);
        try {
            var page = new TweetDetailPage(driver, tweetToUpdate.getLink());
            page.waitForLoad();
            Metrics m = page.getMetrics();

            log.info("Tweet: {}, Replies: {}, Reposts: {}, Likes: {}, Views: {}", tweetToUpdate.getId(), m.getReplies(), m.getReposts(), m.getLikes(), m.getViews());
            tweetToUpdate.setCommentCount(m.getReplies());
            tweetToUpdate.setRepostCount(m.getReposts());
            tweetToUpdate.setLikeCount(m.getLikes());
            tweetToUpdate.setViews(m.getViews());
            tweetToUpdate.setUpdateDate(LocalDateTime.now());
            tweetToUpdate.setNeedsRefresh(false);
        } catch (Exception e) {
            log.error("Nie udalo sie zupdateowac tweeta o id {}: {}", tweetId, e.getMessage());
        }
        return tweetToUpdate;
    }
}
