package pl.bgnat.master.xsnts.exporter.service.exporters;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;
import pl.bgnat.master.xsnts.sentiment.repository.SentimentResultRepository;
import pl.bgnat.master.xsnts.exporter.utils.CsvWriterUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SentimentResultExporter implements Exporter {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] HEAD_SENTIMENT = {
            "tweet_id","username","normalized_content","token_count",
            "sentiment_label","sentiment_score",
            "token_strategy","sentiment_model_strategy",
            "likes","reposts","comments","views","post_date","analysis_date"
    };

    private final SentimentResultRepository sentimentRepo;


    @Override
    public String export(TokenStrategyLabel tokenStrategy,
                         SentimentStrategyLabel sentimentModelStrategy,
                         String userPath) throws IOException {

        List<SentimentResult> results = sentimentRepo.findByTokenStrategyAndSentimentModelStrategy(
                tokenStrategy, sentimentModelStrategy);

        System.out.println("=== EXPORT DEBUG ===");
        System.out.println("JPA pobrało rekordów: " + results.size());

        String path = CsvWriterUtil.defaultName(String.format("sentiment_%s_%s", tokenStrategy.name().toLowerCase(),
                        sentimentModelStrategy.name().toLowerCase()), userPath, "sentiment");

        int processedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        try (FileWriter w = CsvWriterUtil.open(path)) {
            CsvWriterUtil.writeLine(w, HEAD_SENTIMENT);

            for (SentimentResult r : results) {
                try {
                    ProcessedTweet p = r.getProcessedTweet();
                    if (p == null) {
                        System.err.println("SKIP: SentimentResult ID=" + r.getId() + " - brak ProcessedTweet");
                        skippedCount++;
                        continue;
                    }

                    Tweet t = p.getOriginalTweet();
                    if (t == null) {
                        System.err.println("SKIP: SentimentResult ID=" + r.getId() + " - brak OriginalTweet");
                        skippedCount++;
                        continue;
                    }

                    // Sprawdź czy daty nie są NULL
                    if (t.getPostDate() == null) {
                        System.err.println("SKIP: Tweet ID=" + t.getId() + " - postDate is NULL");
                        skippedCount++;
                        continue;
                    }

                    if (r.getAnalysisDate() == null) {
                        System.err.println("SKIP: SentimentResult ID=" + r.getId() + " - analysisDate is NULL");
                        skippedCount++;
                        continue;
                    }

                    CsvWriterUtil.writeLine(w,
                            String.valueOf(t.getId()),
                            CsvWriterUtil.esc(t.getUsername()),
                            CsvWriterUtil.esc(p.getNormalizedContent()),
                            String.valueOf(p.getTokenCount()),
                            r.getLabel().name(),
                            String.valueOf(r.getScore()),
                            r.getTokenStrategy().name(),
                            r.getSentimentModelStrategy().name(),
                            String.valueOf(t.getLikeCount()),
                            String.valueOf(t.getRepostCount()),
                            String.valueOf(t.getCommentCount()),
                            String.valueOf(t.getViews()),
                            CsvWriterUtil.esc(t.getPostDate().format(ISO)),
                            CsvWriterUtil.esc(r.getAnalysisDate().format(ISO))
                    );

                    processedCount++;

                    if (processedCount % 10000 == 0) {
                        System.out.println("Przetworzono: " + processedCount + " rekordów...");
                    }

                } catch (Exception e) {
                    errorCount++;
                    System.err.println("ERROR: SentimentResult ID=" + r.getId() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("=== EXPORT SUMMARY ===");
        System.out.println("JPA pobrało: " + results.size());
        System.out.println("Zapisano do CSV: " + processedCount);
        System.out.println("Pominięto: " + skippedCount);
        System.out.println("Błędów: " + errorCount);
        System.out.println("Całkowite straty: " + (results.size() - processedCount));

        return path;
    }

}
