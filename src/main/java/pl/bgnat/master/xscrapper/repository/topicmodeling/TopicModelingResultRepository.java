package pl.bgnat.master.xscrapper.repository.topicmodeling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.topicmodeling.TopicModelingResult;

import java.util.List;

@Repository
public interface TopicModelingResultRepository extends JpaRepository<TopicModelingResult, Long> {

    List<TopicModelingResult> findByStatusOrderByTrainingDateDesc(TopicModelingResult.ModelStatus status);

}
