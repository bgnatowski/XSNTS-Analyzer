package pl.bgnat.master.xsnts.sentiment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;

import java.util.List;
import java.util.Set;

@Repository
public interface SentimentResultRepository extends JpaRepository<SentimentResult, Long> {

    String QUERY_SELECT_SENTIMENT_RESULTS = """
            SELECT sr FROM SentimentResult sr
            JOIN FETCH sr.processedTweet p
            JOIN FETCH p.originalTweet t
            """;
    String QUERY_SELECT_ALL_PROCESSED_BY_TWEET_ID_IN_LIST = "SELECT sr FROM SentimentResult sr WHERE sr.processedTweet.originalTweet.id IN :tweetIds";

    @Query(QUERY_SELECT_SENTIMENT_RESULTS)
    List<SentimentResult> findAllWithDependencies();

    @Query(QUERY_SELECT_ALL_PROCESSED_BY_TWEET_ID_IN_LIST)
    List<SentimentResult> findAllByProcessedTweetIdIn(@Param("tweetIds") Set<Long> tweetIds);

    List<SentimentResult> findAllByProcessedTweetIdInAndTokenStrategyAndSentimentModelStrategy(Set<Long> longs, TokenStrategyLabel tokenStrategyLabel, SentimentStrategyLabel sentimentStrategyLabel);

    @Query("SELECT s FROM SentimentResult s " +
            "JOIN FETCH s.processedTweet p " +
            "JOIN FETCH p.originalTweet t " +
            "WHERE s.tokenStrategy = :tokenStrategy " +
            "AND s.sentimentModelStrategy = :sentimentModelStrategy")
    List<SentimentResult> findByTokenStrategyAndSentimentModelStrategy(
            @Param("tokenStrategy") TokenStrategyLabel tokenStrategy,
            @Param("sentimentModelStrategy") SentimentStrategyLabel sentimentModelStrategy);

    @Modifying
    @Transactional
    int deleteByTokenStrategyAndSentimentModelStrategy(
            TokenStrategyLabel tokenStrategy,
            SentimentStrategyLabel modelStrategy);
}
