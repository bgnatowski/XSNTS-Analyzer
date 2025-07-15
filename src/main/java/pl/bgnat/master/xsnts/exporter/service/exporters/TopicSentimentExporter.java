package pl.bgnat.master.xsnts.exporter.service.exporters;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.exporter.utils.CsvWriterUtil;
import pl.bgnat.master.xsnts.sentiment.model.TopicSentimentStatsEntity;
import pl.bgnat.master.xsnts.sentiment.repository.TopicSentimentStatsRepository;

import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TopicSentimentExporter implements Exporter {

    private static final String[] HEADERS = {
            "model_id", "token_strategy", "sentiment_model_strategy",
            "topic_id", "topic_result_label",
            "positive", "neutral", "negative", "total_tweets",
            "positive_ratio", "negative_ratio", "avg_score",
            "calculated_at"
    };

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TopicSentimentStatsRepository topicSentimentStatsRepository;

    /**
     * Eksportuje wszystkie rekordy stats dla podanego modelu
     *
     * @param modelId  – ID TopicModelingResult
     * @param userPath – katalog docelowy (null ⇒ bieżący)
     * @return pełna ścieżka pliku CSV
     */
    @Override
    public String export(Long modelId, String userPath) throws Exception {
        List<TopicSentimentStatsEntity> rows = topicSentimentStatsRepository.findByModelIdOrderByTopicId(modelId);

        String file = CsvWriterUtil.defaultName(
                "topic_sentiment_stats_" + modelId, userPath, "csv");

        try (FileWriter w = CsvWriterUtil.open(file)) {
            CsvWriterUtil.writeLine(w, HEADERS);

            for (TopicSentimentStatsEntity e : rows) {
                CsvWriterUtil.writeLine(w,
                        String.valueOf(e.getModelId()),
                        e.getTokenStrategy().name(),
                        e.getSentimentModelStrategy().name(),
                        String.valueOf(e.getTopicId()),
                        CsvWriterUtil.esc(e.getTopicResultLabel()),
                        String.valueOf(e.getPositive()),
                        String.valueOf(e.getNeutral()),
                        String.valueOf(e.getNegative()),
                        String.valueOf(e.getTotalTweets()),
                        String.valueOf(e.getPositiveRatio()),
                        String.valueOf(e.getNegativeRatio()),
                        String.valueOf(e.getAvgScore()),
                        e.getCalculatedAt().format(ISO)
                );
            }
        }
        return file;
    }
}
