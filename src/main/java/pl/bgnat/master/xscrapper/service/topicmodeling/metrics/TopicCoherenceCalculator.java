package pl.bgnat.master.xscrapper.service.topicmodeling.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TopicCoherenceCalculator {

    /**
     * Oblicza PMI dla pary słów zgodnie ze wzorem z pracy magisterskiej
     */
    public double calculatePMI(String word1, String word2, Map<String, Integer> wordCounts,
                               Map<String, Integer> cooccurrenceCounts, int totalDocs) {

        double pWi = (double) wordCounts.getOrDefault(word1, 0) / totalDocs;
        double pWj = (double) wordCounts.getOrDefault(word2, 0) / totalDocs;
        double pWiWj = (double) cooccurrenceCounts.getOrDefault(word1 + "_" + word2, 0) / totalDocs;

        double epsilon = 1.0;
        return Math.log((pWiWj + epsilon) / (pWi * pWj)) / Math.log(2);
    }

    public double calculateNPMI(String word1, String word2, Map<String, Integer> wordCounts,
                                Map<String, Integer> cooccurrenceCounts, int totalDocs) {

        double pmi = calculatePMI(word1, word2, wordCounts, cooccurrenceCounts, totalDocs);
        double pWiWj = (double) cooccurrenceCounts.getOrDefault(word1 + "_" + word2, 0) / totalDocs;

        if (pWiWj == 0) return 0.0;
        return pmi / (-Math.log(pWiWj) / Math.log(2));
    }

    public double calculateUMassCoherence(String word1, String word2,
                                          Map<String, Set<String>> wordDocuments) {

        Set<String> docsWithWi = wordDocuments.getOrDefault(word1, new HashSet<>());
        Set<String> docsWithBoth = new HashSet<>(docsWithWi);
        docsWithBoth.retainAll(wordDocuments.getOrDefault(word2, new HashSet<>()));

        double epsilon = 1.0;
        return Math.log((docsWithBoth.size() + epsilon) / docsWithWi.size());
    }

    public double calculateUCICoherence(List<String> topWords, Map<String, Integer> wordCounts,
                                        Map<String, Integer> cooccurrenceCounts, int totalDocs) {

        int N = topWords.size();
        double sum = 0.0;
        int pairCount = 0;

        for (int i = 0; i < N - 1; i++) {
            for (int j = i + 1; j < N; j++) {
                sum += calculatePMI(topWords.get(i), topWords.get(j),
                        wordCounts, cooccurrenceCounts, totalDocs);
                pairCount++;
            }
        }

        return (2.0 / (N * (N - 1))) * sum;
    }


}
