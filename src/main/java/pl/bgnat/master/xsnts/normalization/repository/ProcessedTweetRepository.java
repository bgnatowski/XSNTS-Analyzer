package pl.bgnat.master.xsnts.normalization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface ProcessedTweetRepository extends JpaRepository<ProcessedTweet, Long> {

    String QUERY_FIND_ALL_PROCESSED_TWEET_IDS =
            """
                SELECT pt.originalTweet.id FROM ProcessedTweet pt
            """;
    String QUERY_CALCULATE_AVG_TOKEN_COUNT =
            """
                SELECT AVG(pt.tokenCount) FROM ProcessedTweet pt
                WHERE pt.tokenCount IS NOT NULL
            """;
    String QUERY_FIND_EMPTY_RECORDS =
            """ 
                SELECT pt FROM ProcessedTweet pt WHERE
                (pt.normalizedContent IS NULL OR pt.normalizedContent = '') OR
                (pt.tokens IS NULL OR pt.tokens = '')
            """;
    String QUERY_COUNT_EMPTY_RECORDS =
            """
               SELECT COUNT(pt) FROM ProcessedTweet pt WHERE
               (pt.normalizedContent IS NULL OR pt.normalizedContent = '') OR
               (pt.tokens IS NULL OR pt.tokens = '')
            """;
    String QUERY_DELETE_EMPTY_RECORDS =
            """
                DELETE FROM ProcessedTweet pt WHERE
                (pt.normalizedContent IS NULL OR pt.normalizedContent = '') OR
                (pt.tokens IS NULL OR pt.tokens = '')
            """;
    String QUERY_FIND_BY_ORG_TWEET_ID_IN_LIST =
            """
                SELECT pt FROM ProcessedTweet pt
                WHERE pt.originalTweet.id IN :tweetIds
            """;
    String QUERY_DELETE_BY_IDS =
            """
                DELETE FROM ProcessedTweet pt WHERE pt.id IN :ids
            """;
    String QUERY_FIND_BY_DATE_RANGE =
            """
                SELECT pt FROM ProcessedTweet pt
                WHERE pt.originalTweet.postDate BETWEEN :start AND :end
            """;

    String QUERY_FIND_BY_DATE_AFTER =
            """
                SELECT pt
                FROM   ProcessedTweet pt
                WHERE  pt.originalTweet.postDate >= :start
            """;

    String QUERY_FIND_BY_DATE_BEFORE =
             """
                SELECT pt
                FROM   ProcessedTweet pt
                WHERE  pt.originalTweet.postDate <= :end
             """;

    @Query(QUERY_FIND_BY_DATE_AFTER)
    List<ProcessedTweet> findByOriginalTweet_PostDateAfter(@Param("start") LocalDateTime start);

    @Query(QUERY_FIND_BY_DATE_BEFORE)
    List<ProcessedTweet> findByOriginalTweet_PostDateBefore(@Param("end") LocalDateTime end);

    @Query(QUERY_FIND_BY_DATE_RANGE)
    List<ProcessedTweet> findAllInDateRange(@Param("start") LocalDateTime start, @Param("end")   LocalDateTime end);

    @Query(QUERY_FIND_ALL_PROCESSED_TWEET_IDS)
    Set<Long> findAllProcessedTweetIds();

    @Query(QUERY_CALCULATE_AVG_TOKEN_COUNT)
    Long getAverageTokenCount();

    @Query(QUERY_FIND_EMPTY_RECORDS)
    List<ProcessedTweet> findEmptyRecords();

    @Query(QUERY_COUNT_EMPTY_RECORDS)
    long countEmptyRecords();

    @Modifying
    @Transactional
    @Query(QUERY_DELETE_EMPTY_RECORDS)
    int deleteEmptyRecords();

    @Modifying
    @Transactional
    @Query(QUERY_DELETE_BY_IDS)
    int deleteByIds(@Param("ids") List<Long> ids);

    @Query(QUERY_FIND_BY_ORG_TWEET_ID_IN_LIST)
    List<ProcessedTweet> findAllByOriginalTweetIdIn(@Param("tweetIds") Set<Long> tweetIds);

}
