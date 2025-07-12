package pl.bgnat.master.xsnts.scrapper.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface TweetRepository extends JpaRepository<Tweet, Long> {

    @Query("""
        SELECT t.id
        FROM Tweet t
        WHERE t.needsRefresh = true
          AND t.updateDate < :cutoff
        ORDER BY t.updateDate ASC
        """)
    List<Long> findIdsToRefresh(
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    @Query("""
        SELECT t.id
        FROM Tweet t
        ORDER BY t.updateDate ASC
        """)
    List<Long> findOldestTweetIds(Pageable pageable);

    boolean existsByLink(String link);

    List<Tweet> findByLinkIn(Set<String> links);
}
