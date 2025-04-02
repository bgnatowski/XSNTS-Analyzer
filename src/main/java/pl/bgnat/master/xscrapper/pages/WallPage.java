package pl.bgnat.master.xscrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.model.Tweet;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasLength;
import static pl.bgnat.master.xscrapper.utils.TweetParser.*;
import static pl.bgnat.master.xscrapper.utils.TweetParser.parseCountFromAriaLabel;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.*;

@Slf4j
public class WallPage extends BasePage {
    private static final int MAX_TWEETS_PER_SCRAPE = 1000;

    public enum WallType {
        FOR_YOU, POPULAR, NEWEST
    }

    public WallPage(WebDriver driver) {
        super(driver);
        zoomOutAndReturn();
    }

    public void openForYou(){
        openSubPage("/home");
    }

    public void openPopular(String keyword){
        String searchUrl = "/search?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        openSubPage(searchUrl);
    }

    public void openNewest(String keyword){
        String searchUrl = "/search?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8) + "&f=live";
        openSubPage(searchUrl);
    }

    public Set<Tweet> scrapeTweets() {
        Set<Tweet> tweets = new HashSet<>();
        while (true) {
            try {
                waitRandom();
                if (checkForErrorAndStop()) {
                    break;
                }

                scrollToBottom();
                waitRandom();

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
            } catch (Exception e) {
                refreshPage();
                log.warn("Wystąpił błąd przy scrapowaniu tweetów; Odświeżam stronę.");
            }
            clickNewPostsButtonIfExists();
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

        Long commentCount = parseCountFromAriaLabel(tweetElement, "reply");
        tweet.setCommentCount(commentCount);

        Long repostCount = parseCountFromAriaLabel(tweetElement, "retweet");
        tweet.setRepostCount(repostCount);

        Long likeCount = parseCountFromAriaLabel(tweetElement, "like");
        tweet.setLikeCount(likeCount);

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
            WebElement newPostsButton = findElement(newPostsButtonLocator);
            log.info("Szukam przycisku 'See new posts'.");
            if (newPostsButton != null && newPostsButton.isDisplayed() && newPostsButton.isEnabled()) {
                newPostsButton.click();
                log.info("Kliknięto przycisk 'See new posts'.");
            }
        } catch (NoSuchElementException | TimeoutException e) {
            log.info("Przycisk 'See new posts' nie został znaleziony.");
        } catch (Exception e) {
            log.error("Błąd podczas próby kliknięcia przycisku 'See new posts'.", e);
        }
    }

}
