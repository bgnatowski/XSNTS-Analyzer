package pl.bgnat.master.xsnts.exporter.service.exporters;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.exporter.utils.CsvWriterUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Component
@RequiredArgsConstructor
public class ProcessedTweetExporter implements Exporter {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] HEAD_PROCESSED = { "processed_tweet_id", "tweet_id","username",
            "normalized_content","tokens", "lemmatized_tokens", "token_count","like_count", "repost_count", "comment_count", "views", "post_date" };

    private static final int PAGE_SIZE = 1000;

    private final ProcessedTweetRepository processedRepo;

    @Override
    public String export(String userPath) throws IOException {
        String path = CsvWriterUtil.defaultName("processed", userPath, "processed");

        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_PROCESSED);
            int page = 0;
            boolean more = true;
            int total = 0;
            while (more) {
                Page<ProcessedTweet> processedPage = processedRepo.findAll(
                        PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
                if(processedPage.getContent().isEmpty()) break;
                for (ProcessedTweet p : processedPage.getContent()) {
                    Tweet t = p.getOriginalTweet();
                    CsvWriterUtil.writeLine(w,
                            String.valueOf(p.getId()),
                            t != null ? String.valueOf(t.getId()) : "",
                            t != null ? CsvWriterUtil.esc(t.getUsername()) : "",
                            CsvWriterUtil.esc(p.getNormalizedContent()),
                            CsvWriterUtil.esc(p.getTokens()),
                            CsvWriterUtil.esc(p.getTokensLemmatized()),
                            String.valueOf(p.getTokenCount()),
                            t != null && t.getLikeCount() != null ? CsvWriterUtil.getNumber(t.getLikeCount()) : "0",
                            t != null && t.getRepostCount() != null ? CsvWriterUtil.getNumber(t.getRepostCount()) : "0",
                            t != null && t.getCommentCount() != null ? CsvWriterUtil.getNumber(t.getCommentCount()) : "0",
                            t != null && t.getViews() != null ? CsvWriterUtil.getNumber(t.getViews()) : "0",
                            t != null && t.getPostDate() != null ?
                                    CsvWriterUtil.esc(t.getPostDate().format(ISO)) : ""
                    );
                    total++;
                }
                more = !processedPage.isLast();
                page++;
            }
            System.out.println("Wyeksportowano rekordów: " + total);
        }
        return path;
    }
}
