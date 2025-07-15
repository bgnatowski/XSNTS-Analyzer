package pl.bgnat.master.xscrapper.service.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.dto.normalization.NormalizedTweet;
import pl.bgnat.master.xscrapper.model.normalization.ProcessedTweet;
import pl.bgnat.master.xscrapper.model.scrapper.Tweet;
import pl.bgnat.master.xscrapper.repository.normalization.ProcessedTweetRepository;

import java.time.LocalDateTime;

/**
 * Komponent odpowiedzialny za przetwarzanie pojedynczych tweetów
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TweetProcessor {

    private final TextNormalizer textNormalizer;
    private final ObjectMapper objectMapper;
    private final ProcessedTweetRepository processedTweetRepository;

    /**
     * Przetwarza tweet i zapisuje go do bazy danych
     * @param tweet tweet do przetworzenia
     * @return przetworzony tweet lub null w przypadku błędu
     */
    public ProcessedTweet processAndSave(Tweet tweet) {
        ProcessedTweet processedTweet = processWithoutSave(tweet);
        if (processedTweet != null) {
            return processedTweetRepository.save(processedTweet);
        }
        return null;
    }

    /**
     * Przetwarza tweet bez zapisywania do bazy danych
     * @param tweet tweet do przetworzenia
     * @return przetworzony tweet lub null w przypadku błędu
     */
    public ProcessedTweet processWithoutSave(Tweet tweet) {
        if (tweet.getContent() == null || tweet.getContent().trim().isEmpty()) {
            log.warn("Tweet {} ma pustą treść", tweet.getId());
            return null;
        }

        try {
            NormalizedTweet result = textNormalizer.processText(tweet.getContent());

            return ProcessedTweet.builder()
                    .originalTweet(tweet)
                    .normalizedContent(result.getNormalizedContent())
                    .tokens(convertTokensToJson(result.getTokens()))
                    .tokenCount(result.getTokenCount())
                    .processedDate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania tweeta {}: {}", tweet.getId(), e.getMessage());
            return null;
        }
    }

    private String convertTokensToJson(java.util.List<String> tokens) {
        try {
            return objectMapper.writeValueAsString(tokens);
        } catch (Exception e) {
            log.error("Błąd podczas konwersji tokenów do JSON", e);
            return "[]";
        }
    }
}
