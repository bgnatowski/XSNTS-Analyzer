package pl.bgnat.master.xscrapper.utils;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xscrapper.dto.Metrics;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TweetParser {
    private static final By METRICS_GROUP_XPATH = By.xpath("//div[@role='group' and @aria-label]");
    private static final By POST_LINK_XPATH = By.xpath(".//a[contains(@href, '/status/')]/time/parent::a");
    private static final By POST_DATE_XPATH = By.xpath(".//a[contains(@href, '/status/')]/time");
    private static final By CONTENT_XPATH = By.xpath(".//div[@data-testid='tweetText']");
    private static final By USERNAME_DIV_XPATH = By.xpath(".//div[@data-testid='User-Name']");
    private static final By USERNAME_XPATH = By.xpath(".//span[contains(text(), '@')]");

    private static final Pattern REPLIES_PATTERN = Pattern.compile("(\\d+) (replies|reply)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPOSTS_PATTERN = Pattern.compile("(\\d+) (reposts|repost)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIKES_PATTERN = Pattern.compile("(\\d+) (likes|like)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VIEWS_PATTERN = Pattern.compile("(\\d+) (views|view)", Pattern.CASE_INSENSITIVE);


    public static String parseUsername(WebElement tweetElement) {
        try {
            WebElement userNameDiv = tweetElement.findElement(USERNAME_DIV_XPATH);
            WebElement handleSpan = userNameDiv.findElement(USERNAME_XPATH);

            return handleSpan.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public static String parseTweetContent(WebElement tweetElement) {
        try {
            WebElement contentEl = tweetElement.findElement(CONTENT_XPATH);
            return contentEl.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public static String parsePostLink(WebElement tweetElement) {
        try {
            WebElement postLink = tweetElement.findElement(POST_LINK_XPATH);
            String rawHref = postLink.getDomAttribute("href");

            if (!StringUtils.hasLength(rawHref)) {
                log.warn("rawHref jest pusty");
                return null;
            }
            // Jeśli href nie zawiera "https://" (tylko zaczyna się od "/"), doklej pełny URL
            if (rawHref.startsWith("/")) {
                rawHref = "https://www.x.com" + rawHref;
            }
            return rawHref;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public static LocalDateTime parsePostDate(WebElement tweetElement) {
        String dateTimeStr = null;
        try {
            WebElement timeEl = tweetElement.findElement(POST_DATE_XPATH);
            // Przykład: "2025-01-03T14:26:29.000Z"
            dateTimeStr = timeEl.getDomAttribute("datetime");

            if (!StringUtils.hasLength(dateTimeStr)) {
                log.warn("dateTimeStr jest pusty");
                return null;
            }

            ZonedDateTime zdt = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            return zdt.toLocalDateTime();
        } catch (NoSuchElementException e) {
            return null;
        } catch (DateTimeParseException e) {
            log.warn("Inny format daty: {}", dateTimeStr);
            return null;
        }
    }

    public static Metrics getMetricsForTweet(WebElement tweetElement) {
        try {
            WebElement group = tweetElement.findElement(METRICS_GROUP_XPATH);
            String aria = group.getAttribute("aria-label");

            long replies = findMetricNumber(REPLIES_PATTERN, aria);
            long reposts = findMetricNumber(REPOSTS_PATTERN, aria);
            long likes = findMetricNumber(LIKES_PATTERN, aria);
            long views = findMetricNumber(VIEWS_PATTERN, aria);

            return new Metrics(replies, reposts, likes, views);
        } catch (NoSuchElementException e) {
            log.warn("Nie znaleziono grupy z metrykami", e);
            return Metrics.empty();
        }
    }

    private static long findMetricNumber(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? Long.parseLong(m.group(1)) : 0L;
    }
}
