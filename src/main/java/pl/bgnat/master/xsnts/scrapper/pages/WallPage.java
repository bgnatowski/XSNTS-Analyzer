package pl.bgnat.master.xsnts.scrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import pl.bgnat.master.xsnts.scrapper.dto.Metrics;
import pl.bgnat.master.xsnts.scrapper.dto.UserCredential;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.util.StringUtils.hasLength;
import static pl.bgnat.master.xsnts.scrapper.utils.TweetParser.*;
import static pl.bgnat.master.xsnts.scrapper.utils.WaitUtils.waitRandom;

/**
 * Główna klasa widoku wall (popular, latest, fy) z ktorej scrapowane są tweety.
 */
@Slf4j
public class WallPage extends BasePage {
    private static final int MAX_TWEETS_PER_SCRAPE = 150; // przy większych ilościach częściej następują blokady
    private final UserCredential.User user;

    public enum WallType {
        FOR_YOU, POPULAR, LATEST
    }

    public WallPage(WebDriver driver, UserCredential.User user) {
        super(driver);
        this.user = user;
    }

    public void openForYou(){
        log.info("Otwieram for you dla uzytkownika: {}", user);
        openSubPage("/home");
    }

    public void openPopular(String keyword){
        log.info("Otwieram trendujacy tag: {}, popularne, dla uzytkownika: {}", keyword, user);
        navigateRandomly(keyword);
    }

    public void openLatest(String keyword){
        log.info("Otwieram trendujacy tag: {}, najnowsze, dla uzytkownika: {}", keyword, user);
        navigateRandomlyToLatest(keyword);
    }

    public Set<Tweet> scrapeTweets() {
        Set<Tweet> tweets = new HashSet<>();
        int repeatedBottom = 0;
        while (true) {
            try {
                waitRandom();
                if (checkForErrorAndStop()) {
                    break;
                }

                long lastHeight = smartScroll();

                List<WebElement> scrappedTweetElements = getTweetElements();
                if (!scrappedTweetElements.isEmpty()) {
                    tweets.addAll(
                            scrappedTweetElements.stream()
                                .map(this::parseTweet)
                                .filter(tweet -> hasLength(tweet.getUsername()))
                                .toList())
                    ;
                    log.info("Zebrano nowych tweetow przy scrollu: {}. Aktualna ilosc: {}", scrappedTweetElements.size(), tweets.size());
                    if (tweets.size() >= MAX_TWEETS_PER_SCRAPE) {
                        log.info("Osiaganieto limit tweetElementow. Przerywam petle.");
                        break;
                    }
                } else {
                    log.info("Nie znaleziono tweetów przy scrollowaniu. Ponawiam petle.");
                }

                if(repeatedBottom > 6){
                    log.info("Tweety się nie ładują przez 6 petli. Refresh");
                    break;
                }
                if (repeatedBottom > 10) break;

                long newHeight = smartScroll();
                if (newHeight == lastHeight) {
                    log.info("Tweety się nie ładują. Osiągnięto bottom strony");
                    repeatedBottom++;
                } else {
                    lastHeight = newHeight;
                }

            } catch (Exception e) {
                refreshPage();
                log.warn("Wystąpił błąd przy scrapowaniu tweetów; Odświeżam stronę.");
            }
//            clickNewPostsButtonIfExists();
        }
        log.info("Kończę pętlę endless scroll");
        return tweets;
    }

    private Tweet parseTweet(WebElement tweetElement) {
        Tweet tweet = new Tweet();

        String username = parseUsername(tweetElement);
        if(!hasLength(username)) { return tweet; }
        tweet.setUsername(username);

        String content = parseTweetContent(tweetElement);
        tweet.setContent(content);

        String postLink = parsePostLink(tweetElement);
        tweet.setLink(postLink.trim());

        LocalDateTime postDate = parsePostDate(tweetElement);
        tweet.setPostDate(postDate);

        Metrics m = getMetricsForTweet(tweetElement);
//        log.info("Replies: {}, Reposts: {}, Likes: {}, Views: {}", m.getReplies(), m.getReposts(), m.getLikes(), m.getViews());
        tweet.setCommentCount(m.getReplies());
        tweet.setRepostCount(m.getReposts());
        tweet.setLikeCount(m.getLikes());
        tweet.setViews(m.getViews());

        LocalDateTime now = LocalDateTime.now();
        tweet.setCreationDate(now);
        tweet.setUpdateDate(now);

//        log.info("Parsed tweet: {}", tweet);
        return tweet;
    }

    // Pobiera listę tweetów na stronie – zakładamy, że każdy tweet jest reprezentowany przez article z data-testid="tweet"
    private List<WebElement> getTweetElements() {
        return waitForElements(By.xpath("//article[@data-testid='tweet']"));
    }

    private boolean checkForErrorAndStop() {
        try {
            By errorLocator = By.xpath("//span[text()='Something went wrong. Try reloading.']");

            findElement(errorLocator);
            log.info("Blokada. Wystapilo: 'Something went wrong. Try reloading.'");
            return true;
        } catch (NoSuchElementException elementException) {
//            log.info("Brak zawieszenia z 'Something went wrong. Try reloading.'");
            return false;
        }
    }

    private void clickNewPostsButtonIfExists() {
        try {
            By newPostsButtonLocator = By.xpath("//button[.//div[@data-testid='pillLabel']]");
            WebElement newPostsButton = waitForElement(newPostsButtonLocator);
            if (newPostsButton != null && newPostsButton.isDisplayed() && newPostsButton.isEnabled()) {
                newPostsButton.click();
                log.info("Kliknięto przycisk 'See new posts'.");
            }
        } catch (NoSuchElementException | TimeoutException e) {
//            log.info("Przycisk 'See new posts' nie został znaleziony.");
        } catch (Exception e) {
            log.error("Błąd podczas próby kliknięcia przycisku 'See new posts'.", e);
        }
    }

}
