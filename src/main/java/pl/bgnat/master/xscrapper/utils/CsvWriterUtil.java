package pl.bgnat.master.xscrapper.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CsvWriterUtil {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Zwraca ścieżkę z timestampem, gdy użytkownik nie poda własnej. */
    public static String defaultName(String prefix, String userPath) {
        if (userPath != null && !userPath.isBlank()) return userPath;
        String file = prefix + "_" + LocalDateTime.now().format(TS) + ".csv";
        return Path.of("").toAbsolutePath().resolve(file).toString();
    }

    public static FileWriter open(String filePath) throws IOException {
        return new FileWriter(filePath, StandardCharsets.UTF_8);
    }

    public static String esc(String v) { return v == null ? "\"\"" : '"' + v.replace("\"", "\"\"") + '"'; }

    public static void writeLine(FileWriter w, String... cols) throws IOException {
        for (int i = 0; i < cols.length; i++) {
            w.append(cols[i]);
            if (i < cols.length - 1) w.append(',');
        }
        w.append('\n');
    }
}
