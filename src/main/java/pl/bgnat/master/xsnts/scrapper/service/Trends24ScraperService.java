package pl.bgnat.master.xsnts.scrapper.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Trends24ScraperService {
    private static final String TRENDS24_URL = "https://trends24.in/poland/";

    public List<String> scrapeUniqueTrendingKeywords() {
        try {
            Document doc = Jsoup.connect(TRENDS24_URL).get();

            Elements trendContainers = doc.select(".list-container");

            Element oneHourAgoContainer = findContainerByApproximateTime(trendContainers, 1);
            Element sixHoursAgoContainer = findContainerByApproximateTime(trendContainers, 6);

            if (oneHourAgoContainer == null || sixHoursAgoContainer == null) {
                log.warn("Nie znaleziono kontenerów z trendami dla wymaganych przedziałów czasowych");
                return Collections.emptyList();
            }

            Set<String> oneHourAgoTrends = extractTrends(oneHourAgoContainer);
            Set<String> sixHoursAgoTrends = extractTrends(sixHoursAgoContainer);

            log.info("Znaleziono {} trendów z 1h temu i {} trendów z 6h temu",
                    oneHourAgoTrends.size(), sixHoursAgoTrends.size());

            Set<String> uniqueTrends = new HashSet<>();

            uniqueTrends.addAll(oneHourAgoTrends.stream()
                    .filter(trend -> !sixHoursAgoTrends.contains(trend))
                    .collect(Collectors.toSet()));

            uniqueTrends.addAll(sixHoursAgoTrends.stream()
                    .filter(trend -> !oneHourAgoTrends.contains(trend))
                    .collect(Collectors.toSet()));

            log.info("Znaleziono {} unikalnych trendów", uniqueTrends.size());

            return new ArrayList<>(uniqueTrends);

        } catch (IOException e) {
            log.error("Błąd podczas scrapowania trendów: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Element findContainerByApproximateTime(Elements trendContainers, int hoursAgo) {
        Instant targetTime = Instant.now().minus(hoursAgo, ChronoUnit.HOURS);

        Element closestContainer = null;
        long closestDifference = Long.MAX_VALUE;

        for (Element container : trendContainers) {
            Element timestampElement = container.selectFirst("h3.title");
            if (timestampElement == null) continue;

            String timestampStr = timestampElement.attr("data-timestamp");
            if (timestampStr.isEmpty()) continue;

            try {
                double timestamp = Double.parseDouble(timestampStr);
                Instant containerTime = Instant.ofEpochSecond((long) timestamp);

                long difference = Math.abs(containerTime.getEpochSecond() - targetTime.getEpochSecond());

                if (difference < closestDifference) {
                    closestDifference = difference;
                    closestContainer = container;
                }
            } catch (NumberFormatException e) {
                log.info("Niepoprawny format");
            }
        }

        return closestContainer;
    }

    private Set<String> extractTrends(Element container) {
        Elements trendElements = container.select("a.trend-link");

        return trendElements.stream()
                .map(Element::text)
                .collect(Collectors.toSet());
    }
}
