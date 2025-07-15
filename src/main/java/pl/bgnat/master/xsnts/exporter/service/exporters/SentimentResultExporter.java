package pl.bgnat.master.xsnts.exporter.service.exporters;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;
import pl.bgnat.master.xsnts.sentiment.repository.SentimentResultRepository;
import pl.bgnat.master.xsnts.exporter.utils.CsvWriterUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class SentimentResultExporter implements Exporter {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] HEAD_SENTIMENT = {
            "tweet_id","username","normalized_content","token_count",
            "sentiment_label","sentiment_score",
            "token_strategy","sentiment_model_strategy",
            "likes","reposts","comments","views","post_date"
    };

    private final SentimentResultRepository sentimentRepo;

    @Override
    public String export(String userPath) throws IOException {
        String path = CsvWriterUtil.defaultName("sentiment", userPath, "sentiment");
        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_SENTIMENT);
            for (SentimentResult r : sentimentRepo.findAllWithDependencies()) {
                ProcessedTweet p = r.getProcessedTweet();
                Tweet t = p.getOriginalTweet();
                CsvWriterUtil.writeLine(w,
                        String.valueOf(t.getId()),
                        CsvWriterUtil.esc(t.getUsername()),
                        CsvWriterUtil.esc(p.getNormalizedContent()),
                        String.valueOf(p.getTokenCount()),
                        r.getLabel().name(),
                        r.getTokenStrategy().name(),
                        r.getSentimentModelStrategy().name(),
                        String.valueOf(r.getScore()),
                        String.valueOf(t.getLikeCount()),
                        String.valueOf(t.getRepostCount()),
                        String.valueOf(t.getCommentCount()),
                        String.valueOf(t.getViews()),
                        CsvWriterUtil.esc(t.getPostDate().format(ISO))
                );
            }
        }
        return path;
    }
}
