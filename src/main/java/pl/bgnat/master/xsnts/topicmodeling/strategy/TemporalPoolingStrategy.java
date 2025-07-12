package pl.bgnat.master.xsnts.topicmodeling.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategia grupowania tweetów według czasu (dzień publikacji)
 */
@Slf4j
@Component
public class TemporalPoolingStrategy implements TweetPoolingStrategy {
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getStrategyName() {
        return "temporal";
    }

    @Override
    public Map<String, List<ProcessedTweet>> poolTweets(List<ProcessedTweet> processedTweets) {
        log.info("Rozpoczynam grupowanie {} tweetów według czasu", processedTweets.size());

        Map<String, List<ProcessedTweet>> temporalGroups = processedTweets.stream()
                .collect(Collectors.groupingBy(this::extractDateKey));

        log.info("Utworzono {} grup temporalnych", temporalGroups.size());
        return temporalGroups;
    }


    private String extractDateKey(ProcessedTweet tweet) {
        LocalDateTime postDate = tweet.getOriginalTweet().getPostDate();
        return "day_" + postDate.format(dayFormatter);
    }
}
