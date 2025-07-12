package pl.bgnat.master.xsnts.topicmodeling.strategy;

import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;

import java.util.List;
import java.util.Map;

/**
 * Interfejs dla różnych strategii grupowania tweetów w dokumenty
 */
public interface TweetPoolingStrategy {

    /**
     * Grupuje tweety w dokumenty zgodnie z implementowaną strategią
     * @param processedTweets lista przetworzonych tweetów
     * @return mapa: klucz dokumentu -> lista tweetów w dokumencie
     */
    Map<String, List<ProcessedTweet>> poolTweets(List<ProcessedTweet> processedTweets);

    /**
     * Zwraca nazwę strategii
     * @return nazwa strategii
     */
    String getStrategyName();
}
