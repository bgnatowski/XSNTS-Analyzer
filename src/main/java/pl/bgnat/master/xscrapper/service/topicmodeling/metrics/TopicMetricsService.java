package pl.bgnat.master.xscrapper.service.topicmodeling.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.dto.TopicModelingResponse.WordWeight;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicMetricsService {

    private final TopicCoherenceCalculator calculator;

    /**
     * Buduje mapy DF & co-occur na podstawie dokumentów (lista tokenów per dokument).
     */
    public MetricsContext buildContext(List<List<String>> documents) {

        Map<String, Integer> wordCounts = new HashMap<>();
        Map<String, Integer> coCounts  = new HashMap<>();
        Map<String, Set<String>> wordDocs = new HashMap<>();

        for (int docId = 0; docId < documents.size(); docId++) {
            List<String> tokens = new ArrayList<>(new HashSet<>(documents.get(docId))); // unikalne słowa
            for (String w : tokens) {
                wordCounts.merge(w, 1, Integer::sum);
                wordDocs.computeIfAbsent(w, k -> new HashSet<>())
                        .add(String.valueOf(docId));
            }
            // pary współwystąpień
            for (int i = 0; i < tokens.size() - 1; i++) {
                for (int j = i + 1; j < tokens.size(); j++) {
                    String key = tokens.get(i) + "_" + tokens.get(j);
                    coCounts.merge(key, 1, Integer::sum);
                }
            }
        }
        return new MetricsContext(wordCounts, coCounts, wordDocs, documents.size());
    }

    /**
     * Oblicza komplet metryk dla listy topWords.
     */
    public TopicMetrics computeMetrics(List<WordWeight> topWords,
                                       MetricsContext ctx) {
        List<String> words = topWords.stream()
                .map(WordWeight::getWord)
                .collect(Collectors.toList());

        double pmi  = calculator.calculateUCICoherence(
                words, ctx.wordCounts, ctx.coCounts, ctx.totalDocs);

        // średnia NPMI
        double npmi = pairAverage(words, (w1, w2) ->
                calculator.calculateNPMI(w1, w2,
                        ctx.wordCounts, ctx.coCounts, ctx.totalDocs));

        // średnia UMass
        double umass = pairAverage(words, (w1, w2) ->
                calculator.calculateUMassCoherence(
                        w1, w2, ctx.wordDocuments));

        return new TopicMetrics(pmi, npmi, pmi /*uci==avg PMI*/, umass);
    }

    /* ---------- utils ---------- */

    private double pairAverage(List<String> items,
                               PairFunction f) {
        double sum = 0; int cnt = 0;
        for (int i = 0; i < items.size() - 1; i++) {
            for (int j = i + 1; j < items.size(); j++) {
                sum += f.apply(items.get(i), items.get(j));
                cnt++;
            }
        }
        return cnt == 0 ? 0.0 : sum / cnt;
    }

    @FunctionalInterface
    private interface PairFunction {
        double apply(String w1, String w2);
    }

    public record MetricsContext(
            Map<String,Integer> wordCounts,
            Map<String,Integer> coCounts,
            Map<String,Set<String>> wordDocuments,
            int totalDocs){}

    public record TopicMetrics(
            double pmi, double npmi, double uci, double umass){}
}
