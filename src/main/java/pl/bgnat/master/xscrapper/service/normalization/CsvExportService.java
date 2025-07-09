package pl.bgnat.master.xscrapper.service.normalization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.model.normalization.ProcessedTweet;
import pl.bgnat.master.xscrapper.model.scrapper.Tweet;
import pl.bgnat.master.xscrapper.repository.normalization.ProcessedTweetRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serwis odpowiedzialny za eksport danych do plików CSV
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private static final String CSV_HEADER = "tweet_id,username,original_content,normalized_content,tokens,token_count,post_date\n";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ProcessedTweetRepository processedTweetRepository;

    /**
     * Eksportuje przetworzone tweety do pliku CSV
     * @param filePath ścieżka do pliku wyjściowego
     * @return ścieżka do utworzonego pliku
     * @throws IOException w przypadku błędu zapisu
     */
    public String exportProcessedTweets(String filePath) throws IOException {
        log.info("Rozpoczynam eksport do CSV: {}", filePath);

        List<ProcessedTweet> processedTweets = processedTweetRepository.findAll();

        try (FileWriter writer = new FileWriter(filePath)) {
            writeHeader(writer);
            writeData(writer, processedTweets);
        }

        log.info("Eksport zakończony. Wyeksportowano {} rekordów", processedTweets.size());
        return filePath;
    }

    private void writeHeader(FileWriter writer) throws IOException {
        writer.append(CSV_HEADER);
    }

    private void writeData(FileWriter writer, List<ProcessedTweet> processedTweets) throws IOException {
        for (ProcessedTweet processed : processedTweets) {
            writeCsvRow(writer, processed);
        }
    }

    private void writeCsvRow(FileWriter writer, ProcessedTweet processed) throws IOException {
        Tweet originalTweet = processed.getOriginalTweet();

        writer.append(String.valueOf(originalTweet.getId())).append(",")
                .append(csvEscape(originalTweet.getUsername())).append(",")
                .append(csvEscape(originalTweet.getContent())).append(",")
                .append(csvEscape(processed.getNormalizedContent())).append(",")
                .append(csvEscape(processed.getTokens())).append(",")
                .append(String.valueOf(processed.getTokenCount())).append(",")
                .append(csvEscape(originalTweet.getPostDate().format(DATE_FORMATTER)))
                .append("\n");
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
