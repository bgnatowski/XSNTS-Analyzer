package pl.bgnat.master.xscrapper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.dto.TweetDto;
import pl.bgnat.master.xscrapper.mapper.TweetMapper;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.repository.TweetRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
@AllArgsConstructor
public class TweetService {
    private final TweetRepository tweetRepository;

    public void saveTweets(Set<Tweet> scrappedTweets) {
        Set<Tweet> tweetsList = new HashSet<>();
        int repeatCount = 0;
        for (Tweet tweet : scrappedTweets) {
            if (StringUtils.hasLength(tweet.getLink())) {
                if (!isExists(tweet)) {
                    tweetsList.add(tweet);
                } else {
                    repeatCount ++;
                    log.warn("Tweet istnieje w bazie");
                }
            }
        }

        tweetRepository.saveAll(tweetsList);
        log.info("Zapisano: {} tweetow do bazy z {} zescrapowanych. Ilość powtórzeń: {}", tweetsList.size(), scrappedTweets.size(), repeatCount);
    }

    private TweetDto saveTweet(Tweet tweet){
        log.info("Start saveTweet");
        if(!StringUtils.hasLength(tweet.getLink()))
            return null;

        Tweet saved = tweetRepository.save(tweet);
        return TweetMapper.INSTANCE.toDto(saved);
    }

    private boolean isExists(Tweet tweetObj) {
        String link = tweetObj.getLink();
        return tweetRepository.existsTweetByLink(link);
    }

    private void updateTweet(Tweet tweetObj) {
        String link = tweetObj.getLink();
        Tweet existingTweet = tweetRepository.findByLink(link);
        existingTweet.setContent(tweetObj.getContent());
        existingTweet.setLikeCount(tweetObj.getLikeCount());
        existingTweet.setRepostCount(tweetObj.getRepostCount());
        existingTweet.setCommentCount(tweetObj.getCommentCount());
        existingTweet.setUpdateDate(LocalDateTime.now());
        tweetRepository.save(tweetObj);
    }
}
