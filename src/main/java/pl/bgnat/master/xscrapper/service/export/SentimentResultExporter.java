package pl.bgnat.master.xscrapper.service.export;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.model.normalization.ProcessedTweet;
import pl.bgnat.master.xscrapper.model.scrapper.Tweet;
import pl.bgnat.master.xscrapper.model.sentiment.SentimentResult;
import pl.bgnat.master.xscrapper.repository.sentiment.SentimentResultRepository;
import pl.bgnat.master.xscrapper.utils.CsvWriterUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class SentimentResultExporter implements Exporter {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] HEAD_SENTIMENT = {
            "tweet_id", "username", "normalized_content",
            "token_count", "sentiment_label", "sentiment_score",
            "likes", "reposts", "comments", "views", "post_date"
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
