package pl.bgnat.master.xscrapper.service.export;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CsvExportService {
    private final ProcessedTweetExporter processedTweetExporter;
    private final SentimentResultExporter sentimentResultExporter;
    private final TopicSentimentExporter topicSentimentExporter;

    public String exportProcessedTweets(String userPath) throws Exception {
        return processedTweetExporter.export(userPath);
    }

    public String exportSentimentResults(String userPath) throws Exception {
        return sentimentResultExporter.export(userPath);
    }

    public String exportTopicSentimentTweets(Long topicModelingResultId, String userPath) throws Exception {
        return topicSentimentExporter.export(topicModelingResultId, userPath);
    }
}
