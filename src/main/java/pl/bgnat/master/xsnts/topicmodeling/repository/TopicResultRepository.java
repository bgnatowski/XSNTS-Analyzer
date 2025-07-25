package pl.bgnat.master.xsnts.topicmodeling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicResult;

import java.util.Collection;
import java.util.List;

@Repository
public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
    List<TopicResult> findByTopicModelingResultIdOrderByTopicId(Long topicModelingResultId);

    Collection<TopicResult> findByTopicModelingResultId(Long modelId);
}
