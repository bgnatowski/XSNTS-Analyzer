package pl.bgnat.master.xsnts.exporter.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CsvWriterUtil {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static String defaultName(String prefix, String userPath, String subfolder) {
        String file = prefix + "_" + LocalDateTime.now().format(TS) + ".csv";
        Path dir;
        if (StringUtils.hasLength(userPath)) {
            dir = Path.of(userPath, subfolder);
        } else {
            dir = Path.of("output/csv", subfolder);
        }
        dir.toFile().mkdirs();
        return dir.resolve(file).toString();
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
