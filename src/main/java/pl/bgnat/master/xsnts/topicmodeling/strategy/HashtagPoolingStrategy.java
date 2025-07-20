package pl.bgnat.master.xsnts.topicmodeling.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Grupuje tweety według hashtagów (bez znaku #, małe litery).
 * • Hashtagi krótsze niż 2 znaki są ignorowane.
 * • Tweety bez hashtagów trafiają do grup autora, jeżeli autor ma ≥ minAuthorGroup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HashtagPoolingStrategy implements TweetPoolingStrategy {
    // Dozwolone hashtagi: #litera(min. 2 znaki po #)
    private static final Pattern HASH = Pattern.compile("#\\p{L}[\\p{L}\\p{N}_]{2,}");
    private final ObjectMapper objectMapper;
    private final int minAuthorGroup = 3;

    @Override
    public String getStrategyName() {
        return "hashtag";
    }

    @Override
    public Map<String, List<ProcessedTweet>> poolTweets(
            List<ProcessedTweet> tweets) {

        Map<String, List<ProcessedTweet>> groups = new HashMap<>();
        List<ProcessedTweet> noHash = new ArrayList<>();

        for (ProcessedTweet pt : tweets) {
            Set<String> tags = extractFromTokens(pt.getTokens());

            if (tags.isEmpty()) {
                noHash.add(pt);
            } else {
                for (String tag : tags) {
                    groups.computeIfAbsent(tag, k -> new ArrayList<>()).add(pt);
                }
            }
        }

        // Fallback — grupowanie wg autora
        Map<String, List<ProcessedTweet>> authors = noHash.stream()
                .collect(Collectors.groupingBy(pt ->
                        pt.getOriginalTweet().getUsername()));

        authors.forEach((user, list) -> {
            if (list.size() >= minAuthorGroup) {
                groups.put("author_" + user.toLowerCase(), list);
            }
        });

        log.info("Pooling zakończony: {} grup hashtagów.", groups.size());
        return groups;
    }

    /**
     * Wyszukuje poprawne hashtagi tylko wśród tokenów (każdy token z osobna).
     * Tokeny są przekazywane jako string w formacie np. ["#tag1","słowo","#tag2"]
     */
    private Set<String> extractFromTokens(String tokensField) {
        if (!StringUtils.hasLength(tokensField)) return Collections.emptySet();

        try {
            List<String> tokens = objectMapper.readValue(tokensField,
                    new TypeReference<>() {});

            Set<String> tags = new HashSet<>();

            for (String token : tokens) {
                String trimmed = token.trim();
                if (HASH.matcher(trimmed).matches()) {
                    tags.add(trimmed.substring(1).toLowerCase()); // bez #
                }
            }
            return tags;
        } catch (JsonProcessingException e) {
            log.warn("Błąd brak tokenów.");
            return Collections.emptySet();
        }
    }
}
