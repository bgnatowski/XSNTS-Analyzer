package pl.bgnat.master.xscrapper.repository.sentiment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.sentiment.SentimentResult;

import java.util.List;

@Repository
public interface SentimentResultRepository extends JpaRepository<SentimentResult, Long> {

    @Query("""
    SELECT sr FROM SentimentResult sr
    JOIN FETCH sr.processedTweet p
    JOIN FETCH p.originalTweet t
    """)
    List<SentimentResult> findAllWithDependencies();

}
