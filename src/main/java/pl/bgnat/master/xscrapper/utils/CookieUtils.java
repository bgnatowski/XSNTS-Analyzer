package pl.bgnat.master.xscrapper.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import pl.bgnat.master.xscrapper.dto.CookieDto;
import pl.bgnat.master.xscrapper.mapper.CookieMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.bgnat.master.xscrapper.config.GlobalConfig.objectMapper;

@Slf4j
public class CookieUtils {
    private static final String COOKIES_DIR = "cookies";
    private static final Path COOKIES_DIR_PATH = Paths.get(COOKIES_DIR);

    public static void deleteAllCookiesFiles() {
        validateCookiesDirectory();

        try (Stream<Path> files = Files.walk(COOKIES_DIR_PATH)) {
            files.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            System.out.println("Usunięto plik ciasteczek: " + path.getFileName());
                        } catch (IOException e) {
                            System.err.println("Błąd przy usuwaniu pliku " + path + ": " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Błąd podczas czyszczenia katalogu cookies: " + e.getMessage());
        }
    }

    public static void saveCookiesToFile(WebDriver driver, String fileName) {
        validateCookiesDirectory();

        Set<Cookie> cookies = driver.manage().getCookies();
        List<CookieDto> cookieDtoList = convertCookiesToDtoList(cookies);
        Path filePath = COOKIES_DIR_PATH.resolve(fileName);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), cookieDtoList);
            log.info("Zapisano {} cookies do pliku {}", cookies.size(), filePath);
        } catch (IOException e) {
            log.error("Błąd zapisu cookies do pliku {}: {}", filePath, e.getMessage());
        }
    }

    public static void loadCookiesFromFile(WebDriver driver, String fileName) {
        Path filePath = COOKIES_DIR_PATH.resolve(fileName);

        if (!Files.exists(filePath)) {
            log.info("Plik {} nie istnieje", filePath);
            return;
        }

        try {
            List<CookieDto> cookieDtoList = objectMapper.readValue(filePath.toFile(), new TypeReference<>() {});
            cookieDtoList.stream()
                    .map(CookieMapper.INSTANCE::buildCookie)
                    .forEach(cookie -> driver.manage().addCookie(cookie));

            log.info("Załadowano {} cookies z pliku {}", cookieDtoList.size(), filePath);
        } catch (IOException e) {
            log.error("Błąd wczytywania cookies z pliku {}: {}", filePath, e.getMessage());
        }
    }

    private static List<CookieDto> convertCookiesToDtoList(Set<Cookie> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return Collections.emptyList();
        }
        return cookies.stream()
                .map(CookieMapper.INSTANCE::seleniumCookieToDto)
                .collect(Collectors.toList());
    }

    private static void validateCookiesDirectory() {
        try {
            if (!Files.exists(COOKIES_DIR_PATH)) {
                Files.createDirectories(COOKIES_DIR_PATH);
                log.info("Utworzono katalog: {}", COOKIES_DIR_PATH);
            }
        } catch (IOException e) {
            log.error("Błąd tworzenia katalogu cookies: {}", e.getMessage());
            throw new IllegalStateException("Nie można utworzyć katalogu cookies", e);
        }
    }
}
