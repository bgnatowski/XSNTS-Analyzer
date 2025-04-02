package pl.bgnat.master.xscrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Slf4j
public class TrendingPage extends BasePage {
    private static final String TRENDING_SUB_URL = "/explore/tabs/trending";

    public TrendingPage(WebDriver driver) {
        super(driver);
        zoomOutAndReturn();
    }

    public List<String> scrapeTrendingKeywords() {
        openTrending();

        long lastHeight = 0L;
        String collectedTrends;

        Set<String> trendsKeywordsSet = new LinkedHashSet<>();
        while (true) {
            List<WebElement> trendElements = getTrendElements();
            log.info("Pobrano: {} elementów", trendElements.size());

            trendElements.forEach(webElement -> {
                String keywordNr = extractTrending(webElement);
                trendsKeywordsSet.add(keywordNr);
            });

            if(trendsKeywordsSet.size() >= 30){
                collectedTrends = String.join(" ", trendsKeywordsSet);
                log.info("30 aktualnych trendow: {}", collectedTrends);
                break;
            }

            waitRandom();
            long newHeight = scrollBy1000();

            if (newHeight != lastHeight) {
                log.info("Ponawiam pętle");
                lastHeight = newHeight;
            }
        }
        return extractTrendKeyword(trendsKeywordsSet);
    }

    private void openTrending() {
        openSubPage(TRENDING_SUB_URL);
    }

    // Pobiera elementy trendów – zakładamy, że selektor dotyczy divów z data-testid="trend" i role="link"
    private List<WebElement> getTrendElements() {
        return waitForElements(By.xpath(".//div[@data-testid='trend' and @role='link']"));
    }

    // Wyciąga tekst trendu – przyjmujemy, że interesujący nas tekst (np. nazwa) jest w drugim divie wewnątrz kontenera
    private String extractTrending(WebElement trendElement) {
        try {
            WebElement innerDiv = trendElement.findElement(By.xpath("./div"));
            WebElement nr = innerDiv.findElement(By.xpath("./div//span"));
            WebElement trendTextElement = innerDiv.findElement(By.xpath("./div[2]//span"));

            return nr.getText() + ". " + trendTextElement.getText();
        } catch (Exception e) {
            return "Nie udalo sie wyekstraktowac nazwy z trending page";
        }
    }

    private List<String> extractTrendKeyword(Set<String> trendsKeywords) {
        // Wyodrębniamy tylko część po numeracji
        return trendsKeywords.stream()
                .map(entry -> {
                    String[] parts = entry.split("\\. ", 2);
                    return parts.length == 2 ? parts[1] : null;
                })
                .toList();
    }
}
