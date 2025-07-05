package pl.bgnat.master.xscrapper.service.topicmodeling;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.model.ProcessedTweet;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Strategia grupowania tweetów według hashtagów
 * Zgodnie z literaturą - najskuteczniejsza metoda dla topic modeling
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HashtagPoolingStrategy implements TweetPoolingStrategy {

    private final ObjectMapper objectMapper;
    private final Pattern hashtagPattern = Pattern.compile("#\\w+");

    @Override
    public Map<String, List<ProcessedTweet>> poolTweets(List<ProcessedTweet> processedTweets) {
        log.info("Rozpoczynam groupowanie {} tweetów według hashtagów", processedTweets.size());

        Map<String, List<ProcessedTweet>> hashtagGroups = new HashMap<>();
        List<ProcessedTweet> noHashtagTweets = new ArrayList<>();

        for (ProcessedTweet tweet : processedTweets) {
            Set<String> hashtags = extractHashtags(tweet);

            if (hashtags.isEmpty()) {
                noHashtagTweets.add(tweet);
            } else {
                for (String hashtag : hashtags) {
                    hashtagGroups.computeIfAbsent(hashtag, k -> new ArrayList<>()).add(tweet);
                }
            }
        }

        // Grupowanie tweetów bez hashtagów według autorów
        groupTweetsWithoutHashtags(noHashtagTweets, hashtagGroups);

        log.info("Utworzono {} grup hashtagów", hashtagGroups.size());
        return hashtagGroups;
    }

    @Override
    public String getStrategyName() {
        return "hashtag";
    }

    private Set<String> extractHashtags(ProcessedTweet tweet) {
        Set<String> hashtags = new HashSet<>();

        // Wyciągnij hashtagi z oryginalnej treści
        String originalContent = tweet.getOriginalTweet().getContent();
        if (originalContent != null) {
            Matcher matcher = hashtagPattern.matcher(originalContent);
            while (matcher.find()) {
                String hashtag = matcher.group().toLowerCase().substring(1); // Usuń # i konwertuj na małe litery
                if (hashtag.length() > 2) { // Tylko hashtagi o długości > 2 znaki
                    hashtags.add(hashtag);
                }
            }
        }

        return hashtags;
    }

    private void groupTweetsWithoutHashtags(List<ProcessedTweet> noHashtagTweets,
                                            Map<String, List<ProcessedTweet>> hashtagGroups) {
        // Grupuj tweety bez hashtagów według autorów
        Map<String, List<ProcessedTweet>> authorGroups = noHashtagTweets.stream()
                .collect(Collectors.groupingBy(tweet -> tweet.getOriginalTweet().getUsername()));

        for (Map.Entry<String, List<ProcessedTweet>> entry : authorGroups.entrySet()) {
            if (entry.getValue().size() >= 3) { // Tylko autorzy z przynajmniej 3 tweetami
                String groupKey = "author_" + entry.getKey();
                hashtagGroups.put(groupKey, entry.getValue());
            }
        }
    }
}
