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

    public static void saveCookiesToFile(WebDriver driver, String filePath) {
        Set<Cookie> cookies = driver.manage().getCookies();

        List<CookieDto> cookieDtoList = cookies.stream()
                .map(CookieMapper.INSTANCE::seleniumCookieToDto)
                .collect(Collectors.toList());

        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File("cookies/"+filePath), cookieDtoList);
            log.info("Zapisano {} cookies do pliku {}", cookies.size(), filePath);
        } catch (IOException e) {
            log.error("IOException przy zapisie cookiesow. Message: {}", e.getMessage());
        }
    }

    public static void loadCookiesFromFile(WebDriver driver, String filePath) {
        File file = new File("cookies/"+filePath);
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