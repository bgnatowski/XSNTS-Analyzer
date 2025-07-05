package pl.bgnat.master.xscrapper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.DocumentTopicAssignment;

import java.util.List;

@Repository
public interface DocumentTopicAssignmentRepository extends JpaRepository<DocumentTopicAssignment, Long> {

    List<DocumentTopicAssignment> findByTopicModelingResultId(Long topicModelingResultId);

    List<DocumentTopicAssignment> findByDominantTopicIdAndTopicModelingResultId(Integer topicId, Long modelId);

    @Query("SELECT dta FROM DocumentTopicAssignment dta WHERE dta.documentType = :docType AND dta.topicModelingResult.id = :modelId")
    List<DocumentTopicAssignment> findByDocumentTypeAndModelId(@Param("docType") String documentType, @Param("modelId") Long modelId);

    @Query("SELECT COUNT(dta) FROM DocumentTopicAssignment dta WHERE dta.dominantTopicId = :topicId AND dta.topicModelingResult.id = :modelId")
    Long countDocumentsByTopic(@Param("topicId") Integer topicId, @Param("modelId") Long modelId);
}
