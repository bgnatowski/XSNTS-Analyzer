package pl.bgnat.master.xsnts.sentiment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xsnts.sentiment.model.TopicSentimentStatsEntity;

import java.util.List;

@Repository
public interface TopicSentimentStatsRepository extends JpaRepository<TopicSentimentStatsEntity, Long> {
    List<TopicSentimentStatsEntity> findByModelIdOrderByTopicId(long modelId);
}