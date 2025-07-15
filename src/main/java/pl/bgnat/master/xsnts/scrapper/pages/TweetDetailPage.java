package pl.bgnat.master.xsnts.scrapper.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pl.bgnat.master.xsnts.topicmodeling.dto.Metrics;
import pl.bgnat.master.xsnts.scrapper.utils.TweetParser;
import pl.bgnat.master.xsnts.scrapper.utils.WaitUtils;

import static pl.bgnat.master.xsnts.scrapper.utils.WaitUtils.waitRandom;

/**
 * Klasa pagea pojedynczego tweeta (do updatewania)
 */
@Slf4j
public class TweetDetailPage extends BasePage {
    private static final By METRICS_GROUP_XPATH = By.xpath("//div[@role='group' and @aria-label]");
    private final String tweetUrl;

    public TweetDetailPage(WebDriver driver, String tweetUrl) {
        super(driver);
        this.tweetUrl = tweetUrl;
    }

    public void waitForLoad() {
        open(tweetUrl);
        waitRandom();
    }

    public Metrics getMetrics() {
        WebElement group = WaitUtils.waitForElement(driver, METRICS_GROUP_XPATH);
        return TweetParser.getMetricsForTweet(group);
    }
}
