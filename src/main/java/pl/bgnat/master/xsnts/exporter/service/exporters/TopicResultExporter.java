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

/**
 * Eksportuje tematy pojedynczego modelu LDA do pliku CSV.
 * Plik ląduje domyślnie w: output/csv/topic_model/
 */
@Component
@RequiredArgsConstructor
public class TopicResultExporter implements Exporter {

    private static final String[] HEADER = {
            "model_id",
            "model_name",
            "topic_id",
            "topic_label",
            "word_count",
            "document_count",
            "average_probability",
            "pmi_coherence",
            "npmi_coherence",
            "uci_coherence",
            "umass_coherence",
            "top_words_json"
    };

    private final TopicResultRepository       topicRepo;
    private final TopicModelingResultRepository modelRepo;

    /* -----------------------------------------------------------
       Eksport WYŁĄCZNIE dla jednego modelu (wymagany parametr id)
       ----------------------------------------------------------- */
    @Override
    public String export(Long id, String userPath) throws IOException {

        TopicModelingResult model = modelRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Brak modelu o ID = " + id));

        String path = CsvWriterUtil.defaultName(
                "topics_" + model.getModelName(), userPath, "topic_model");

        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEADER);

            for (TopicResult t : topicRepo
                    .findByTopicModelingResultIdOrderByTopicId(id)) {

                CsvWriterUtil.writeLine(w,
                        String.valueOf(model.getId()),
                        CsvWriterUtil.esc(model.getModelName()),
                        String.valueOf(t.getTopicId()),
                        CsvWriterUtil.esc(t.getTopicLabel()),
                        String.valueOf(t.getWordCount()),
                        String.valueOf(t.getDocumentCount()),
                        String.valueOf(t.getAverageProbability()),
                        String.valueOf(t.getPmiCoherence()),
                        String.valueOf(t.getNpmiCoherence()),
                        String.valueOf(t.getUciCoherence()),
                        String.valueOf(t.getUmassCoherence()),
                        CsvWriterUtil.esc(t.getTopWords())
                );
            }
        }
        return path;
    }

    @Override
    public String export(String userPath) {
        throw new UnsupportedOperationException(
                "TopicResultExporter wymaga podania modelId");
    }
}
