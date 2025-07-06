package pl.bgnat.master.xscrapper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.bgnat.master.xscrapper.model.TopicModelingResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicModelingResultRepository extends JpaRepository<TopicModelingResult, Long> {

    List<TopicModelingResult> findByStatusOrderByTrainingDateDesc(TopicModelingResult.ModelStatus status);

    Optional<TopicModelingResult> findFirstByStatusOrderByTrainingDateDesc(TopicModelingResult.ModelStatus status);

    @Query("SELECT tmr FROM TopicModelingResult tmr WHERE tmr.poolingStrategy = :strategy AND tmr.status = :status")
    List<TopicModelingResult> findByPoolingStrategyAndStatus(@Param("strategy") String strategy,
                                                             @Param("status") TopicModelingResult.ModelStatus status);

    @Query("SELECT COUNT(tmr) FROM TopicModelingResult tmr WHERE tmr.trainingDate >= :startDate")
    Long countModelsTrainedSince(@Param("startDate") LocalDateTime startDate);

}
