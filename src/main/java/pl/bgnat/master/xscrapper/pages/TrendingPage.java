package pl.bgnat.master.xscrapper.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TrendingPage extends BasePage {

    public static final String TRENDING_SUB_URL = "explore/tabs/trending";

    public TrendingPage(WebDriver driver) {
        super(driver);
    }

    public void openTrending(){
        openSubPage(TRENDING_SUB_URL);
    }

    // Pobiera elementy trendów – zakładamy, że selektor dotyczy divów z data-testid="trend" i role="link"
    public List<WebElement> getTrendElements() {
        return waitForElements(By.xpath(".//div[@data-testid='trend' and @role='link']"));
    }

    // Wyciąga tekst trendu – przyjmujemy, że interesujący nas tekst (np. nazwa) jest w drugim divie wewnątrz kontenera
    public String extractTrending(WebElement trendElement) {
        try {
            WebElement innerDiv = trendElement.findElement(By.xpath("./div"));
            WebElement nr = innerDiv.findElement(By.xpath("./div//span"));
            WebElement trendTextElement = innerDiv.findElement(By.xpath("./div[2]//span"));

            return nr.getText() + ". " + trendTextElement.getText();
        } catch (Exception e) {
            return "Nie udalo sie wyekstraktowac nazwy z trending page";
        }
    }

    public List<String> extractTrendKeyword(Set<String> trendsKeywords){
        // Wyodrębniamy tylko część po numeracji
        return trendsKeywords.stream()
                .map(entry -> {
                    String[] parts = entry.split("\\. ", 2);
                    return parts.length == 2 ? parts[1] : null;
                })
                .toList();
    }
}
