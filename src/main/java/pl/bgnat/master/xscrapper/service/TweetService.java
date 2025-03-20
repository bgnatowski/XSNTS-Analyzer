package pl.bgnat.master.xscrapper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.dto.TweetDto;
import pl.bgnat.master.xscrapper.mapper.TweetMapper;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.repository.TweetRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.util.StringUtils.hasLength;
import static pl.bgnat.master.xscrapper.utils.TweetParser.*;

@Service
@Slf4j
@AllArgsConstructor
public class TweetService {
    private final TweetRepository tweetRepository;

    public Tweet parseTweet(WebElement tweetElement) {
        log.info("Start parseTweet");
        Tweet tweet = new Tweet();

        String username = parseUsername(tweetElement);
        if(!hasLength(username)) { return tweet; }
        tweet.setUsername(username);

        String content = parseTweetContent(tweetElement);
        tweet.setContent(content);

        String postLink = parsePostLink(tweetElement);
        tweet.setLink(postLink);

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

        log.info("Parsed tweet: {}", tweet);
        return tweet;
    }

    public TweetDto saveTweet(Tweet tweet){
        log.info("Start saveTweet");
        if(!StringUtils.hasLength(tweet.getLink()))
            return null;

        Tweet saved = tweetRepository.save(tweet);
        return TweetMapper.INSTANCE.toDto(saved);
    }

    public boolean isExists(Tweet tweetObj) {
        String link = tweetObj.getLink();
        return tweetRepository.existsTweetByLink(link);
    }

    public void updateTweet(Tweet tweetObj) {
        String link = tweetObj.getLink();
        Tweet existingTweet = tweetRepository.findByLink(link);
        existingTweet.setContent(tweetObj.getContent());
        existingTweet.setLikeCount(tweetObj.getLikeCount());
        existingTweet.setRepostCount(tweetObj.getRepostCount());
        existingTweet.setCommentCount(tweetObj.getCommentCount());
        existingTweet.setUpdateDate(LocalDateTime.now());
        tweetRepository.save(tweetObj);
    }

    public void saveTweets(List<Tweet> tweetsList) {
        log.info("Saving {} tweets", tweetsList.size());
        tweetRepository.saveAllAndFlush(tweetsList);
    }
}
