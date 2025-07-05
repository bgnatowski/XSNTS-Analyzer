package pl.bgnat.master.xscrapper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.TopicResult;

import java.util.List;

@Repository
public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {

    List<TopicResult> findByTopicModelingResultIdOrderByTopicId(Long topicModelingResultId);

    @Query("SELECT tr FROM TopicResult tr WHERE tr.topicModelingResult.id = :modelId AND tr.documentCount >= :minDocuments")
    List<TopicResult> findSignificantTopics(@Param("modelId") Long modelId, @Param("minDocuments") Integer minDocuments);

    @Query("SELECT tr FROM TopicResult tr WHERE tr.topicModelingResult.id = :modelId ORDER BY tr.averageProbability DESC")
    List<TopicResult> findTopicsByStrength(@Param("modelId") Long modelId);
}
