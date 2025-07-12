package pl.bgnat.master.xsnts.exporter.service.exporters;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.exporter.utils.CsvWriterUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ProcessedTweetExporter implements Exporter {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] HEAD_PROCESSED = { "tweet_id","username","original_content",
            "normalized_content","tokens","token_count","post_date" };

    private final ProcessedTweetRepository processedRepo;

    @Override
    public String export(String userPath) throws IOException {
        String path = CsvWriterUtil.defaultName("processed", userPath, "processed");
        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_PROCESSED);
            for (ProcessedTweet p : processedRepo.findAll()) {
                Tweet t = p.getOriginalTweet();
                CsvWriterUtil.writeLine(w,
                        String.valueOf(t.getId()),
                        CsvWriterUtil.esc(t.getUsername()),
                        CsvWriterUtil.esc(t.getContent()),
                        CsvWriterUtil.esc(p.getNormalizedContent()),
                        CsvWriterUtil.esc(p.getTokens()),
                        String.valueOf(p.getTokenCount()),
                        CsvWriterUtil.esc(t.getPostDate().format(ISO))
                );
            }
        }
        return path;
    }
}
