package pl.bgnat.master.xsnts.normalization.service.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;
import pl.bgnat.master.xsnts.normalization.service.LanguageDetectionService;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.util.StringUtils.*;

/**
 * Komponent odpowiedzialny za przetwarzanie pojedynczych tweetów
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolishTweetProcessor {

    private final LanguageDetectionService languageDetectionService;
    private final TextNormalizer textNormalizer;
    private final TextTokenizer textTokenizer;
    private final ObjectMapper objectMapper;

    /**
     * Przetwarza tweet bez zapisywania do bazy danych (zapisuje po powrocie cały batch)
     * @param tweet tweet do przetworzenia
     * @return przetworzony tweet lub null w przypadku błędu
     */
    public ProcessedTweet processTweet(Tweet tweet) {
        if (!hasLength(tweet.getContent())) {
            log.debug("Tweet {} ma pustą treść", tweet.getId());
            return null;
        }

        if(!languageDetectionService.isPolish(tweet.getContent().trim())) {
            log.debug("Tweet {} nie jest po polsku", tweet.getId());
            return null;
        }

        try {
            String normalizedText = textNormalizer.normalize(tweet.getContent());
            List<String> tokens = textTokenizer.tokenize(normalizedText);
            List<String> lemmatizedTokens = textTokenizer.lemmatizeTokens(tokens);

            return ProcessedTweet.builder()
                    .originalTweet(tweet)
                    .normalizedContent(normalizedText)
                    .tokens(convertTokensToJson(tokens))
                    .tokensLemmatized(convertTokensToJson(lemmatizedTokens))
                    .tokenCount(tokens.size())
                    .processedDate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania tweeta {}: {}", tweet.getId(), e.getMessage());
            return null;
        }
    }

    private String convertTokensToJson(List<String> tokens) {
        try {
            return objectMapper.writeValueAsString(tokens);
        } catch (Exception e) {
            log.error("Błąd podczas konwersji tokenów do JSON", e);
            return "[]";
        }
    }
}
