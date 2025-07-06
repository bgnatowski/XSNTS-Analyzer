package pl.bgnat.master.xscrapper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bgnat.master.xscrapper.dto.UserCredential;
import pl.bgnat.master.xscrapper.service.scrapper.ScrapperService;

import java.util.List;

@RestController
@RequestMapping("/api/scrapper")
@RequiredArgsConstructor
public class ScrapperController {

    private final ScrapperService scrapperService;

    @PostMapping("/popular")
    public void scrapePopular(@RequestBody List<String> keywords) {
        scrapperService.scheduledScrapePopularKeywords(keywords);
    }

    @PostMapping("/latest")
    public void scrapeLatest(@RequestBody List<String> keywords) {
        scrapperService.scheduledScrapeLatestKeywords(keywords);
    }

    @PostMapping("/one")
    public void scrapeOneByKeyword(
            @RequestParam String keyword,
            @RequestParam String wallType,
            @RequestParam UserCredential.User user) {
        scrapperService.scrapeOneByKeyword(keyword, wallType, user);
    }

    @PostMapping("/manual/popular")
    public void manualScrapePopular() {
        scrapperService.scheduledScrapePopularKeywords();
    }

    @PostMapping("/manual/latest")
    public void manualScrapeLatest() {
        scrapperService.scheduledScrapeLatestKeywords();
    }

    @PostMapping("/manual/for-you")
    public void manualScrapeForYou() {
        scrapperService.scheduledScrapeForYouWallAsync();
    }
}
