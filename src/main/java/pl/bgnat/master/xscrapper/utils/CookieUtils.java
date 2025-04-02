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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.bgnat.master.xscrapper.config.GlobalConfig.objectMapper;

@Slf4j
public class CookieUtils {
    public enum CookieUsers {
        USER_1, USER_2, USER_3, USER_4, USER_5
    }

    private static final Map<CookieUsers, String> COOKIE_FILE_PATHS =
            Map.of(CookieUsers.USER_1, "cookies/cookies1.json",
                    CookieUsers.USER_2, "cookies/cookies2.json",
                    CookieUsers.USER_3, "cookies/cookies3.json",
                    CookieUsers.USER_4, "cookies/cookies4.json",
                    CookieUsers.USER_5, "cookies/cookies5.json");

    // Kopiuje ciasteczka z drivera źródłowego do docelowego, otwierając najpierw BASE_URL w driverze docelowym
    public static void copyCookies(WebDriver source, WebDriver destination, String baseUrl) {
//        destination.get(baseUrl);
        Set<Cookie> cookies = source.manage().getCookies();
        for (Cookie cookie : cookies) {
            destination.manage().addCookie(cookie);
        }
        destination.navigate().refresh();
        WaitUtils.waitRandom();
    }

    public static void saveCookiesToFile(WebDriver driver, CookieUsers cookieUser) {
        String filePath = COOKIE_FILE_PATHS.get(cookieUser);
        Set<Cookie> cookies = driver.manage().getCookies();

        List<CookieDto> cookieDtoList = cookies.stream()
                .map(CookieMapper.INSTANCE::seleniumCookieToDto)
                .collect(Collectors.toList());

        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filePath), cookieDtoList);
            log.info("Zapisano {} cookies do pliku {}", cookies.size(), filePath);
        } catch (IOException e) {
            log.error("IOException przy zapisie cookiesow. Message: {}", e.getMessage());
        }
    }

    public static void loadCookiesFromFile(WebDriver driver, CookieUsers cookieUser) {
        String filePath = COOKIE_FILE_PATHS.get(cookieUser);
        File file = new File(filePath);
        if (!file.exists()) {
            log.info("Plik {} nie istnieje. Brak cookies do wczytania.", filePath);
            return;
        }

        try {
            List<CookieDto> cookieDtoList = objectMapper.readValue(file, new TypeReference<>() {});

            for (CookieDto dto : cookieDtoList) {
                Cookie cookie = CookieMapper.INSTANCE.buildCookie(dto);
                driver.manage().addCookie(cookie);
            }
            log.info("Załadowano {} cookies z pliku {}", cookieDtoList.size(), filePath);
        } catch (IOException e) {
            log.error("IOException przy wczytywaniu cookiesow. Message: {}", e.getMessage());
        }
    }
}