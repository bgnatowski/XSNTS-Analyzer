package pl.bgnat.master.xscrapper.service.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import pl.bgnat.master.xscrapper.model.normalization.ProcessedTweet;
import pl.bgnat.master.xscrapper.model.scrapper.Tweet;
import pl.bgnat.master.xscrapper.model.sentiment.SentimentResult;
import pl.bgnat.master.xscrapper.repository.normalization.ProcessedTweetRepository;
import pl.bgnat.master.xscrapper.repository.sentiment.SentimentResultRepository;
import pl.bgnat.master.xscrapper.utils.CsvWriterUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] HEAD_PROCESSED = { "tweet_id","username","original_content",
            "normalized_content","tokens","token_count","post_date" };
    private static final String[] HEAD_SENTIMENT = {
            "tweet_id", "username", "normalized_content",
            "token_count", "sentiment_label", "sentiment_score",
            "likes", "reposts", "comments", "views", "post_date"
    };


    private final ProcessedTweetRepository processedRepo;
    private final SentimentResultRepository sentimentRepo;

    public String exportProcessedTweets(String userPath) throws IOException {
        String path = CsvWriterUtil.defaultName("processed", userPath);
        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_PROCESSED);
            for (ProcessedTweet p : processedRepo.findAll()) writeProcessed(w, p);
        }
        log.info("Eksport processed: {}", path);
        return path;
    }

    public String exportSentimentResults(String userPath) throws IOException {
        String path = CsvWriterUtil.defaultName("sentiment", userPath);
        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_SENTIMENT);
            for (SentimentResult r : sentimentRepo.findAllWithDependencies()) writeSentiment(w, r);
        }
        log.info("Eksport sentiment: {}", path);
        return path;
    }

    /* ----------------- prywatne ----------------- */

    private void writeProcessed(FileWriter w, ProcessedTweet p) throws IOException {
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

    private void writeSentiment(FileWriter w, SentimentResult r) throws IOException {
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
