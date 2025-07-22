package pl.bgnat.master.xsnts.exporter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xsnts.exporter.service.exporters.ProcessedTweetExporter;
import pl.bgnat.master.xsnts.exporter.service.exporters.SentimentResultExporter;
import pl.bgnat.master.xsnts.exporter.service.exporters.TopicResultExporter;
import pl.bgnat.master.xsnts.exporter.service.exporters.TopicSentimentExporter;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentRequest;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;

@Service
@RequiredArgsConstructor
public class CsvExportService {
    private final ProcessedTweetExporter processedTweetExporter;
    private final SentimentResultExporter sentimentResultExporter;
    private final TopicSentimentExporter topicSentimentExporter;
    private final TopicResultExporter topicResultExporter;

    public String exportProcessedTweets(String userPath) throws Exception {
        return processedTweetExporter.export(userPath);
    }

    public String exportSentimentResults(String userPath, SentimentRequest request) throws Exception {
        return sentimentResultExporter.export(request.tokenStrategy(), request.sentimentModelStrategy(), userPath);
    }

    public String exportTopicSentimentTweets(Long topicModelingResultId, String userPath) throws Exception {
        return topicSentimentExporter.export(topicModelingResultId, userPath);
    }

    public String exportTopicResults(Long modelId, String userPath) throws Exception {
        return topicResultExporter.export(modelId, userPath);
    }
}
