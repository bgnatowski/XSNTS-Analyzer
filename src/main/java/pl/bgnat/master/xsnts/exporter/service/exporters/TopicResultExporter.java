package pl.bgnat.master.xsnts.exporter.service.exporters;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.exporter.utils.CsvWriterUtil;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicModelingResult;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicResult;
import pl.bgnat.master.xsnts.topicmodeling.repository.TopicModelingResultRepository;
import pl.bgnat.master.xsnts.topicmodeling.repository.TopicResultRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Eksportuje tematy pojedynczego modelu LDA do pliku CSV.
 * Plik ląduje domyślnie w: output/csv/topic_model/
 */
@Component
@RequiredArgsConstructor
public class TopicResultExporter implements Exporter {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String[] HEADER = {
            /* ---- dane modelu ---- */
            "model_id", "model_name", "token_strategy", "pooling_strategy",
            "number_of_topics", "documents_count", "original_tweets_count",
            "training_date", "model_pmi", "model_npmi", "model_uci",
            "model_umass", "model_perplexity",
            /* ---- dane tematu ---- */
            "topic_id", "topic_label", "word_count", "document_count",
            "average_probability", "top_words_json"
    };

    private final TopicResultRepository topicResultRepository;
    private final TopicModelingResultRepository topicModelingResultRepository;

    @Override
    public String export(Long modelId, String userPath) throws IOException {

        TopicModelingResult model = topicModelingResultRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Brak modelu o ID = " + modelId));

        List<TopicResult> topics =
                topicResultRepository.findByTopicModelingResultIdOrderByTopicId(modelId);

        String path = CsvWriterUtil.defaultName(
                "topics_" + model.getModelName(), userPath, "topic_model");

        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEADER);

            for (TopicResult t : topics) {

                CsvWriterUtil.writeLine(w,
                        /* ---- model ---- */
                        String.valueOf(model.getId()),
                        CsvWriterUtil.esc(model.getModelName()),
                        CsvWriterUtil.esc(model.getTokenStrategy()),
                        CsvWriterUtil.esc(model.getPoolingStrategy()),
                        String.valueOf(topics.size()),
                        String.valueOf(model.getDocumentsCount()),
                        String.valueOf(model.getOriginalTweetsCount()),
                        model.getTrainingDate() != null
                                ? CsvWriterUtil.esc(model.getTrainingDate().format(ISO))
                                : "NULL",
                        format(model.getPmi()),
                        format(model.getNpmi()),
                        format(model.getUci()),
                        format(model.getUmass()),
                        format(model.getPerplexity()),
                        /* ---- temat ---- */
                        String.valueOf(t.getTopicId()),
                        CsvWriterUtil.esc(t.getTopicLabel()),
                        String.valueOf(t.getWordCount()),
                        String.valueOf(t.getDocumentCount()),
                        String.valueOf(t.getAverageProbability()),
                        CsvWriterUtil.esc(t.getTopWords())
                );
            }
        }
        return path;
    }

    /* pomocnicze – zamienia null na „NULL” */
    private String format(Double d) {
        return d != null ? String.valueOf(d) : "NULL";
    }
}
