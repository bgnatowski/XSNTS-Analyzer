package pl.bgnat.master.xsnts.sentiment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;

import java.util.List;
import java.util.Set;

@Repository
public interface SentimentResultRepository extends JpaRepository<SentimentResult, Long> {

    @Query("""
    SELECT sr FROM SentimentResult sr
    JOIN FETCH sr.processedTweet p
    JOIN FETCH p.originalTweet t
    """)
    List<SentimentResult> findAllWithDependencies();

    @Query("SELECT sr FROM SentimentResult sr WHERE sr.processedTweet.originalTweet.id IN :tweetIds")
    List<SentimentResult> findAllByProcessedTweetIdIn(@Param("tweetIds") Set<Long> tweetIds);

    long countByProcessedTweetIdNotIn(Set<Long> longs);
}
