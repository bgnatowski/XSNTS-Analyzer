package pl.bgnat.master.xscrapper.service.scrapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.dto.TweetDto;
import pl.bgnat.master.xscrapper.mapper.TweetMapper;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.repository.TweetRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TweetService {
    private final TweetRepository tweetRepository;

    public void saveTweets(Set<Tweet> scrappedTweets) {
        Set<Tweet> tweetsList = new HashSet<>();
        int repeatCount = 0;
        for (Tweet tweet : scrappedTweets) {
            if (StringUtils.hasLength(tweet.getLink())) {
                if (!tweetRepository.existsByLink(tweet.getLink())) {
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

    public Tweet findTweetById(Long id) {
        return tweetRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    public TweetDto saveTweet(Tweet tweet) {
        Tweet saved = tweetRepository.save(tweet);
        return TweetMapper.INSTANCE.toDto(saved);
    }

    public List<Long> findOldestTweetIds(int limit) {
        return tweetRepository.findOldestTweetIds(PageRequest.of(0, limit));
    }

    public List<Long> findIdsToRefresh(LocalDateTime cutoff, int limit) {
        return tweetRepository.findIdsToRefresh(cutoff, PageRequest.of(0, limit));
    }

    public List<Tweet> findAllByIds(List<Long> ids) {
        return tweetRepository.findAllById(ids);
    }

    public boolean existsTweetByLink(String link) {
        return tweetRepository.existsByLink(link);
    }

    public void updateTweets(Set<Tweet> updatedTweets) {
        tweetRepository.saveAll(updatedTweets);
    }
}

