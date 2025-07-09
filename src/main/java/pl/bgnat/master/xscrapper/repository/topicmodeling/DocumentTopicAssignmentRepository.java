package pl.bgnat.master.xscrapper.repository.topicmodeling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.topicmodeling.DocumentTopicAssignment;

import java.util.List;

@Repository
public interface DocumentTopicAssignmentRepository extends JpaRepository<DocumentTopicAssignment, Long> {

    List<DocumentTopicAssignment> findByTopicModelingResultId(Long topicModelingResultId);

    @Query("SELECT COUNT(dta) FROM DocumentTopicAssignment dta WHERE dta.dominantTopicId = :topicId AND dta.topicModelingResult.id = :modelId")
    Long countDocumentsByTopic(@Param("topicId") Integer topicId, @Param("modelId") Long modelId);
}
