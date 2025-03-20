package pl.bgnat.master.xscrapper.utils;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitForElements;

@Slf4j
public class TweetParser {
    public static String parseUsername(WebElement tweetElement) {
        try {
            WebElement userNameDiv = tweetElement.findElement(By.xpath(".//div[@data-testid='User-Name']"));
            WebElement handleSpan = userNameDiv.findElement(By.xpath(".//span[contains(text(), '@')]"));

            return handleSpan.getText();
        } catch (NoSuchElementException e) {
            log.warn("Nie znaleziono user handle (span z @). Być może inny format/rekalama.");
            return null;
        }
    }

    public static String parseTweetContent(WebElement tweetElement) {
        try {
            WebElement contentEl = tweetElement.findElement(By.xpath(".//div[@data-testid='tweetText']"));
            return contentEl.getText();
        } catch (NoSuchElementException e) {
            log.warn("Nie znaleziono contentu: {}", tweetElement);
            return null;
        }
    }

    public static String parsePostLink(WebElement tweetElement) {
        try {
            WebElement timeEl = tweetElement.findElement(By.xpath(".//a[contains(@href, '/status/')]/time/parent::a"));
            String rawHref = timeEl.getDomAttribute("href");

            if(!StringUtils.hasLength(rawHref)){
                log.warn("rawHref jest pusty");
                return null;
            }
            // Jeśli href nie zawiera "https://" (tylko zaczyna się od "/"), doklej pełny URL
            if (rawHref.startsWith("/")) {
                rawHref = "https://www.x.com" + rawHref;
            }
            return rawHref;
        } catch (NoSuchElementException e) {
            log.warn("Nie znaleziono linku: {}", tweetElement);
            return null;
        }
    }

    public static LocalDateTime parsePostDate(WebElement tweetElement) {
        String dateTimeStr = null;
        try {
            WebElement timeEl = tweetElement.findElement(By.xpath(".//a[contains(@href, '/status/')]/time"));
            // Przykład: "2025-01-03T14:26:29.000Z"
            dateTimeStr = timeEl.getDomAttribute("datetime");

            if(!StringUtils.hasLength(dateTimeStr)) {
                log.warn("dateTimeStr jest pusty");
                return null;
            }

            ZonedDateTime zdt = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            return zdt.toLocalDateTime();
        } catch (NoSuchElementException e) {
            log.warn("Nie znaleziono daty: {}", tweetElement);
            return null;
        } catch (DateTimeParseException e) {
            log.warn("Inny format daty: {}", dateTimeStr);
            return null;
        }
    }

    public static Long parseCountFromAriaLabel(WebElement tweetElement, String dataTestId) {
        try {
            WebElement button = tweetElement.findElement(By.xpath(".//button[@data-testid='" + dataTestId + "']"));

            String ariaLabel = button.getDomAttribute("aria-label");
            if (!StringUtils.hasLength(ariaLabel)) {
                return 0L;
            }

            return parseExactNumberFromAriaLabel(ariaLabel);
        } catch (NoSuchElementException e) {
            log.warn("Nie znaleziono: {}. Element: {}", dataTestId, tweetElement);
            return 0L;
        }
    }

    private static Long parseExactNumberFromAriaLabel(String ariaLabel) {
        if (ariaLabel == null) {
            return 0L;
        }

        String text = ariaLabel.toLowerCase().replace("\u00A0", " ");
        text = text.replaceAll(",", ".");

        //  - grupa liczbowa (\d+(\.\d+)?) -> np. 2, 2.5, 2395
        //  - (?:\s*tys\.?)? -> "opcjonalnie spacja + tys.
        // np. "2 tys." -> dopasuje "2 tys."
        //     "3.5 tys." -> "3.5 tys."
        //     "2395 polubień" -> "2395"
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)(\\s*tys\\.?)?");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String numberPart = matcher.group(1);    // np. "2" lub "2.5" lub "2395"
            String tysPart    = matcher.group(3);    // np. " tys." albo null

            double value;
            try {
                value = Double.parseDouble(numberPart);
            } catch (NumberFormatException e) {
                return 0L;
            }

            // Jeśli jest "tys." -> pomnóż x 1000
            if (tysPart != null && !tysPart.isBlank()) {
                value = value * 1000;
            }

            return Math.round(value);
        }
        return 0L;
    }
}
