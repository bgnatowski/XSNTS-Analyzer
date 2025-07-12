package pl.bgnat.master.xsnts.topicmodeling.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mapstruct.ap.internal.util.Strings.isEmpty;

/**
 * Grupuje tweety według hashtagów (bez znaku #, małe litery).
 *  • Hashtagi krótsze niż 3 znaki są ignorowane.
 *  • Tweety bez hashtagów trafiają do grup autora, jeżeli autor ma ≥ minAuthorGroup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HashtagPoolingStrategy implements TweetPoolingStrategy {
    // pattern na 3 znakowe hasztagi aby uniknąć np: #pl, #uk, #nl, #ok ktore mogą generować szum
    private static final Pattern HASH = Pattern.compile("#\\p{L}[\\p{L}\\p{N}_]{2,}");

    private final int minAuthorGroup = 3;

    @Override
    public String getStrategyName() {
        return "hashtag";
    }

    @Override
    public Map<String, List<ProcessedTweet>> poolTweets(List<ProcessedTweet> tweets) {

        Map<String, List<ProcessedTweet>> groups = new HashMap<>();
        List<ProcessedTweet> noHash = new ArrayList<>();

        for (ProcessedTweet pt : tweets) {

            Set<String> tags = extract(pt.getNormalizedContent());

            if (tags.isEmpty()) noHash.add(pt);
            else tags.forEach(tag ->
                    groups.computeIfAbsent(tag, k -> new ArrayList<>()).add(pt));
        }

        // fallback — grupowanie wg autora
        Map<String, List<ProcessedTweet>> authors = noHash.stream().collect(Collectors.groupingBy(t -> t.getOriginalTweet().getUsername()));

        authors.forEach((user, list) -> {
            if (list.size() >= minAuthorGroup) {
                groups.put("author_" + user.toLowerCase(), list);
            }
        });

        log.info("Pooling zakończony: {} grup hashtagów, {} tweetów bez hashtaga.", groups.size(), noHash.size());
        return groups;
    }

    private Set<String> extract(String content) {
        if (isEmpty(content)) return Set.of();
        Matcher m = HASH.matcher(content);
        Set<String> tags = new HashSet<>();
        while (m.find()) tags.add(m.group().substring(1).toLowerCase());
        return tags;
    }
}
