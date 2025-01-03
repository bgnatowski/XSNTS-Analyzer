package pl.bgnat.master.xscrapper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.Tweet;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {
}
