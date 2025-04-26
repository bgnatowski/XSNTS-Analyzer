package pl.bgnat.master.xscrapper.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.Tweet;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {
    boolean existsTweetByLink(String link);
    Tweet findByLink(String link);

        @Query("SELECT t FROM Tweet t WHERE t.content IS NOT NULL ORDER BY t.updateDate ASC")
        Page<Tweet> findTweetsForUpdate(Pageable pageable);
    }

