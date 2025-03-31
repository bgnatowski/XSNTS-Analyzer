package pl.bgnat.master.xscrapper.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import pl.bgnat.master.xscrapper.dto.CookieDto;
import pl.bgnat.master.xscrapper.mapper.CookieMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.bgnat.master.xscrapper.config.GlobalConfig.objectMapper;

@Slf4j
public class CookieUtils {
    private static final String COOKIE_FILE = "cookies.json";

    // Kopiuje ciasteczka z drivera źródłowego do docelowego, otwierając najpierw BASE_URL w driverze docelowym
    public static void copyCookies(WebDriver source, WebDriver destination, String baseUrl) {
        destination.get(baseUrl);
        Set<Cookie> cookies = source.manage().getCookies();
        for (Cookie cookie : cookies) {
            destination.manage().addCookie(cookie);
        }
        destination.navigate().refresh();
        WaitUtils.waitRandom();
    }

    public static void saveCookiesToFile(WebDriver driver) {
        Set<Cookie> cookies = driver.manage().getCookies();

        List<CookieDto> cookieDtoList = cookies.stream()
                .map(CookieMapper.INSTANCE::seleniumCookieToDto)
                .collect(Collectors.toList());

        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(COOKIE_FILE), cookieDtoList);
            log.info("Zapisano {} cookies do pliku {}", cookies.size(), COOKIE_FILE);
        } catch (IOException e) {
            log.error("IOException przy zapisie cookiesow. Message: {}", e.getMessage());
        }
    }

    public static void loadCookiesFromFile(WebDriver driver) {
        File file = new File(COOKIE_FILE);
        if (!file.exists()) {
            log.info("Plik {} nie istnieje. Brak cookies do wczytania.", COOKIE_FILE);
            return;
        }

        try {
            List<CookieDto> cookieDtoList = objectMapper.readValue(file, new TypeReference<>() {});

            for (CookieDto dto : cookieDtoList) {
                Cookie cookie = CookieMapper.INSTANCE.buildCookie(dto);
                driver.manage().addCookie(cookie);
            }
            log.info("Załadowano {} cookies z pliku {}", cookieDtoList.size(), COOKIE_FILE);
        } catch (IOException e) {
            log.error("IOException przy wczytywaniu cookiesow. Message: {}", e.getMessage());
        }
    }
}