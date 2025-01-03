package pl.bgnat.master.xscrapper.service;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.model.Tweet;

import java.time.LocalDateTime;

import static org.springframework.util.StringUtils.hasLength;
import static pl.bgnat.master.xscrapper.utils.TweetParser.*;
import static pl.bgnat.master.xscrapper.utils.TweetParser.parseCountFromAriaLabel;

@Service
@Slf4j
@NoArgsConstructor
public class TweetService {
    public Tweet parseTweet(WebElement tweetElement) {
        log.info("Start parseTweet");
        Tweet tweet = new Tweet();

        log.info("Before parseUsername");
        String username = parseUsername(tweetElement);
        if(!hasLength(username)) { return tweet; }

        tweet.setUsername(username);
        log.info("After parseUsername");

        log.info("Before parseTweetContent");
        String content = parseTweetContent(tweetElement);
        tweet.setContent(content);
        log.info("After parseTweetContent");

        log.info("Before parsePostLink");
        String postLink = parsePostLink(tweetElement);
        tweet.setLink(postLink);
        log.info("After parsePostLink");

        log.info("Before parsePostDate");
        LocalDateTime postDate = parsePostDate(tweetElement);
        tweet.setPostDate(postDate);
        log.info("After parsePostDate");

        log.info("Before parseReply");
        Long commentCount = parseCountFromAriaLabel(tweetElement, "reply");
        tweet.setCommentCount(commentCount);
        log.info("After parseReply");

        log.info("Before parseRetweet");
        Long repostCount = parseCountFromAriaLabel(tweetElement, "retweet");
        tweet.setRepostCount(repostCount);
        log.info("After parseRetweet");

        log.info("Before parseLike");
        Long likeCount   = parseCountFromAriaLabel(tweetElement, "like");
        tweet.setLikeCount(likeCount);
        log.info("After parseLike");

        log.info("Parsed tweet: {}", tweet);
        return tweet;
    }
}
