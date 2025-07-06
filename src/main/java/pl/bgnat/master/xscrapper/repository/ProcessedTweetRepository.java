package pl.bgnat.master.xscrapper.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xscrapper.model.ProcessedTweet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProcessedTweetRepository extends JpaRepository<ProcessedTweet, Long> {

    Optional<ProcessedTweet> findByOriginalTweetId(Long tweetId);

    boolean existsByOriginalTweetId(Long tweetId);

    // NOWE OPTYMALIZACJE - pobieranie tylko ID
    @Query("SELECT pt.originalTweet.id FROM ProcessedTweet pt")
    Set<Long> findAllProcessedTweetIds();

    // OPTYMALIZACJA - średnia tokenów bez pobierania wszystkich rekordów
    @Query("SELECT AVG(pt.tokenCount) FROM ProcessedTweet pt WHERE pt.tokenCount IS NOT NULL")
    Long getAverageTokenCount();

    // Istniejące metody dla pustych rekordów
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

    @Query("SELECT pt FROM ProcessedTweet pt WHERE pt.originalTweet.postDate BETWEEN :startDate AND :endDate")
    List<ProcessedTweet> findByOriginalTweetPostDateBetween(@Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(pt) FROM ProcessedTweet pt WHERE pt.tokenCount > :minTokens")
    Long countTweetsWithMinimumTokens(@Param("minTokens") Integer minTokens);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProcessedTweet pt WHERE pt.id IN :ids")
    int deleteByIds(@Param("ids") List<Long> ids);

    @Query("SELECT pt FROM ProcessedTweet pt ORDER BY pt.processedDate DESC")
    List<ProcessedTweet> findTop50000ByOrderByProcessedDateDesc(Pageable pageable);

    default List<ProcessedTweet> findTop50000ByOrderByProcessedDateDesc() {
        return findTop50000ByOrderByProcessedDateDesc(PageRequest.of(0, 50000));
    }

}
