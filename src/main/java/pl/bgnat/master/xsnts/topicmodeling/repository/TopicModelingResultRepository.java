package pl.bgnat.master.xsnts.topicmodeling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicModelingResult;

import java.util.List;

@Repository
public interface TopicModelingResultRepository extends JpaRepository<TopicModelingResult, Long> {

    List<TopicModelingResult> findByStatusOrderByTrainingDateDesc(TopicModelingResult.ModelStatus status);
}
