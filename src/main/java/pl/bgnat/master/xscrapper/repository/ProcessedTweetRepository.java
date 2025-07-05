package pl.bgnat.master.xscrapper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xscrapper.model.ProcessedTweet;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedTweetRepository extends JpaRepository<ProcessedTweet, Long> {

    Optional<ProcessedTweet> findByOriginalTweetId(Long tweetId);

    boolean existsByOriginalTweetId(Long tweetId);

    long countByTokenCountGreaterThan(Integer minTokens);

    @Query("SELECT pt FROM ProcessedTweet pt WHERE " +
            "(pt.normalizedContent IS NULL OR pt.normalizedContent = '') OR " +
            "(pt.tokens IS NULL OR pt.tokens = '')")
    List<ProcessedTweet> findEmptyRecords();

    @Query("SELECT COUNT(pt) FROM ProcessedTweet pt WHERE " +
            "(pt.normalizedContent IS NULL OR pt.normalizedContent = '') OR " +
            "(pt.tokens IS NULL OR pt.tokens = '')")
    long countEmptyRecords();

    @Modifying
    @Transactional
    @Query("DELETE FROM ProcessedTweet pt WHERE " +
            "(pt.normalizedContent IS NULL OR pt.normalizedContent = '') OR " +
            "(pt.tokens IS NULL OR pt.tokens = '')")
    int deleteEmptyRecords();
}
