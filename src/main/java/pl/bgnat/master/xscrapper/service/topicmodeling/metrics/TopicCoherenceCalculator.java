package pl.bgnat.master.xscrapper.service.topicmodeling.metrics;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Kalkulator zaawansowanych metryk koherencji tematów (PMI, NPMI, UCI)
 * zgodnie z literaturą (Mimno 2011, Röder 2015).
 */
@Slf4j
@Component
public class TopicCoherenceCalculator {

    /* =====================  API  ===================== */

    /**
     * Zwraca trzy metryki koherencji obliczone na podstawie
     * listy słów-top (n-gramów) oraz zbioru dokumentów.
     *
     * @param topWords   lista słów (kolejność ma znaczenie: najpierw top-N)
     * @param documents  lista dokumentów (pojedynczy dokument to ciąg tokenów
     *                   rozdzielonych spacją; wielkość liter bez znaczenia)
     */
    public CoherenceMetrics calculateCoherence(List<String> topWords,
                                               List<String> documents) {

        // Wstępna weryfikacja
        if (topWords == null || topWords.size() < 2 || documents == null || documents.isEmpty()) {
            return emptyMetrics();
        }

        // 1. Zbuduj statystyki współ-wystąpień
        WordCooccurrenceStats stats = buildStats(topWords, documents);

        // 2. Oblicz metryki
        double pmi  = meanPairwise(topWords, (w1, w2) -> pmi(w1, w2, stats));
        double npmi = meanPairwise(topWords, (w1, w2) -> npmi(w1, w2, stats));
        double uci  = meanPairwise(topWords, (w1, w2) -> uci(w1, w2, stats));

        log.debug("Coherence – PMI: {}, NPMI: {}, UCI: {}", pmi, npmi, uci);

        return CoherenceMetrics.builder()
                .pmi(pmi)
                .npmi(npmi)
                .uci(uci)
                .wordCount(topWords.size())
                .documentCount(stats.totalDocuments)
                .build();
    }


    /* =====================  IMPLEMENTACJA  ===================== */

    /** Oblicza średnią funkcji f dla wszystkich par (bez powtórzeń). */
    private double meanPairwise(List<String> words, ScoreFunction f) {
        double sum = 0;
        int pairs = 0;
        for (int i = 0; i < words.size() - 1; i++) {
            for (int j = i + 1; j < words.size(); j++) {
                double v = f.apply(words.get(i), words.get(j));
                if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                    sum += v;
                    pairs++;
                }
            }
        }
        return pairs == 0 ? 0.0 : sum / pairs;
    }

    /* ----------  Właściwe wzory  ---------- */

    private double pmi(String w1, String w2, WordCooccurrenceStats s) {
        double p12 = s.coProb(w1, w2);
        double p1  = s.prob(w1);
        double p2  = s.prob(w2);
        return (p12 == 0 || p1 == 0 || p2 == 0) ? Double.NaN
                : Math.log(p12 / (p1 * p2));
    }

    private double npmi(String w1, String w2, WordCooccurrenceStats s) {
        double p12 = s.coProb(w1, w2);
        if (p12 == 0) return Double.NaN;
        double val = pmi(w1, w2, s) / (-Math.log(p12));
        return Double.isFinite(val) ? val : Double.NaN;
    }

    /** UMass / UCI coherence (log z poprawką +1). */
    private double uci(String w1, String w2, WordCooccurrenceStats s) {
        int d12 = s.coCount(w1, w2);
        int d2  = s.count(w2);
        return d2 == 0 ? Double.NaN
                : Math.log((d12 + 1.0) / d2);
    }

    /* ----------  Budowanie statystyk  ---------- */

    private WordCooccurrenceStats buildStats(List<String> topWords, List<String> docs) {
        Map<String, Integer> wc  = new HashMap<>();
        Map<String, Integer> cc  = new HashMap<>();
        Set<String> topSet       = new HashSet<>(topWords);

        for (String doc : docs) {
            // Zbiór słów z top-N obecnych w dokumencie (bez duplikatów)
            Set<String> present = Arrays.stream(doc.toLowerCase().split("\\s+"))
                    .filter(topSet::contains)
                    .collect(Collectors.toSet());

            // Aktualizuj liczniki pojedynczych słów
            present.forEach(w -> wc.merge(w, 1, Integer::sum));

            // Wszystkie nieuporządkowane pary słów
            List<String> list = new ArrayList<>(present);
            for (int i = 0; i < list.size() - 1; i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    String a = list.get(i);
                    String b = list.get(j);
                    String key = a.compareTo(b) < 0 ? a + '|' + b : b + '|' + a;
                    cc.merge(key, 1, Integer::sum);
                }
            }
        }
        return new WordCooccurrenceStats(wc, cc, docs.size());
    }

    /* ----------  Struktury danych  ---------- */

    private interface ScoreFunction { double apply(String w1, String w2); }

    private WordCooccurrenceStats emptyStats() {
        return new WordCooccurrenceStats(Collections.emptyMap(),
                Collections.emptyMap(), 1);
    }

    private CoherenceMetrics emptyMetrics() {
        return CoherenceMetrics.builder()
                .pmi(Double.NaN).npmi(Double.NaN).uci(Double.NaN)
                .wordCount(0).documentCount(0).build();
    }

    @Builder
    @Data
    public static class CoherenceMetrics {
        private Double pmi;
        private Double npmi;
        private Double uci;
        private Integer wordCount;
        private Integer documentCount;

        public String getInterpretation() {
            if (pmi == null || Double.isNaN(pmi)) return "Brak danych";
            if (pmi > 0.5)  return "Bardzo wysoka koherencja";
            if (pmi > 0.0)  return "Wysoka koherencja";
            if (pmi > -0.5) return "Umiarkowana koherencja";
            if (pmi > -1.0) return "Niska koherencja";
            return "Bardzo niska koherencja";
        }
    }

    /* Pakiet-prywatna klasa pomocnicza do statystyk */
    static class WordCooccurrenceStats {
        private final Map<String, Integer> wordCounts;
        private final Map<String, Integer> coCounts;
        private final int totalDocuments;

        WordCooccurrenceStats(Map<String, Integer> wordCounts,
                              Map<String, Integer> coCounts,
                              int totalDocuments) {
            this.wordCounts      = wordCounts;
            this.coCounts        = coCounts;
            this.totalDocuments  = totalDocuments;
        }

        /* Prawdopodobieństwo wystąpienia słowa */
        double prob(String w) { return (double) count(w) / totalDocuments; }

        /* Prawdopodobieństwo współ-wystąpienia pary słów */
        double coProb(String w1, String w2) { return (double) coCount(w1, w2) / totalDocuments; }

        int count(String w) { return wordCounts.getOrDefault(w, 0); }

        int coCount(String w1, String w2) {
            String key = w1.compareTo(w2) < 0 ? w1 + '|' + w2 : w2 + '|' + w1;
            return coCounts.getOrDefault(key, 0);
        }
    }
}
