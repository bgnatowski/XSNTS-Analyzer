package pl.bgnat.master.xscrapper.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    public static String readResourceFile(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    public static void copyResourceFile(String resourcePath, File targetFile) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
        FileCopyUtils.copy(content, targetFile);
    }

    public static void writeFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
