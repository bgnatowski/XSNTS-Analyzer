package pl.bgnat.master.xscrapper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.TopicResult;

import java.util.List;

@Repository
public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
    List<TopicResult> findByTopicModelingResultIdOrderByTopicId(Long topicModelingResultId);
}
