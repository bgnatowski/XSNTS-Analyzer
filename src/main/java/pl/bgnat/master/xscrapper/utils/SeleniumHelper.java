package pl.bgnat.master.xscrapper.utils;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitForElement;

@Slf4j
public class SeleniumHelper {
    public static void clickButtonIfExists(WebDriver driver, String xpath) {
        WebElement button = waitForElement(driver, By.xpath(xpath));
        if(button != null){
            button.click();
            log.info("Kliknięto przycisk.");
        }
        log.info("Przycisk nie istnieje na stronie.");

    }
}
