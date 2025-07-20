package pl.bgnat.master.xsnts.topicmodeling.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import pl.bgnat.master.xsnts.topicmodeling.dto.CoherenceMetrics;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Kalkulator zaawansowanych metryk spójności tematów, zaimplementowany
 * zgodnie ze wzorami z pracy magisterskiej. Uwzględnia PMI, NPMI, UMass i UCI.
 */
@Slf4j
@Component
public class TopicCoherenceCalculator {

    /**
     * Stała wygładzająca, aby uniknąć logarytmowania zera w PMI i UMass.
     * Zgodna z wartością &epsilon; = 1 w podanych wzorach.
     */
    private static final double EPSILON = 1.0;

    /**
     * Główna metoda obliczeniowa. Zwraca cztery metryki spójności obliczone
     * na podstawie listy słów kluczowych tematu oraz korpusu dokumentów.
     *
     * @param topWords  Lista N najważniejszych słów dla danego tematu.
     * @param documents Lista dokumentów (jako stringi tokenów oddzielonych spacją),
     *                  na podstawie których budowane są statystyki.
     * @return Obiekt CoherenceMetrics zawierający obliczone wartości.
     */
    public CoherenceMetrics calculateAllMetrics(List<String> topWords,
                                                List<String> documents) {
        // Wstępna walidacja danych wejściowych
        if (CollectionUtils.isEmpty(topWords) || topWords.size() < 2 || CollectionUtils.isEmpty(documents)) {
            return CoherenceMetrics.empty();
        }

        // 1. Zbuduj statystyki słów i ich współwystąpień
        WordCooccurrenceStats stats = buildStats(topWords, documents);

        // 2. Oblicz średnie wartości metryk dla wszystkich par słów
        double avgPmi = meanPairwise(topWords, (w1, w2) -> pmi(w1, w2, stats));
        double avgNpmi = meanPairwise(topWords, (w1, w2) -> npmi(w1, w2, stats));
        double avgUmass = meanPairwise(topWords, (w1, w2) -> umass(w1, w2, stats));

        // 3. Oblicz metrykę UCI (która jest średnią z PMI)
        double uci = avgPmi;

        log.debug("Obliczone metryki spójności – avgPMI (UCI): {}, avgNPMI: {}, avgUMass: {}", uci, avgNpmi, avgUmass);

        return CoherenceMetrics.builder()
                .pmi(avgPmi)
                .npmi(avgNpmi)
                .umass(avgUmass)
                .uci(uci)
                .wordCount(topWords.size())
                .documentCount(stats.totalDocuments)
                .build();
    }

    /**
     * Oblicza średnią wartość funkcji `f` dla wszystkich unikalnych par słów z listy.
     */
    private double meanPairwise(List<String> words, ScoreFunction f) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < words.size() - 1; i++) {
            for (int j = i + 1; j < words.size(); j++) {
                double score = f.apply(words.get(i), words.get(j));
                if (!Double.isNaN(score) && !Double.isInfinite(score)) {
                    sum += score;
                    count++;
                }
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    /* ======================================================================
     *  Implementacje wzorów metryk
     * ====================================================================== */

    /**
     * Oblicza Wzajemną Informację Punktową (PMI) z wygładzaniem.
     * Wzór: log2( (P(wi, wj) * D + epsilon) / (P(wi) * P(wj)) ) -- interpretacja z użyciem liczności.
     */
    private double pmi(String w1, String w2, WordCooccurrenceStats s) {
        double pWi = s.prob(w1);
        double pWj = s.prob(w2);
        double pWij = s.coProb(w1, w2);

        if (pWi <= 0 || pWj <= 0 || pWij <= 0) return Double.NaN;

        return Math.log(pWij / (pWi * pWj)) / Math.log(2);
    }

    /**
     * Oblicza Znormalizowaną Wzajemną Informację Punktową (NPMI).
     * Wzór: PMI(wi, wj) / -log2(P(wi, wj))
     */
    private double npmi(String w1, String w2, WordCooccurrenceStats s) {
        double pmiScore = pmi(w1, w2, s);
        double pWij = s.coProb(w1, w2);

        if (pWij <= 0) return Double.NaN;
        double normalizer = -Math.log(pWij) / Math.log(2);

        return normalizer != 0 ? pmiScore / normalizer : Double.NaN;
    }

    /**
     * Oblicza spójność UMass.
     * Wzór: log( (D(wi, wj) + epsilon) / D(wi) )
     */
    private double umass(String w1, String w2, WordCooccurrenceStats s) {
        int dWj = s.count(w2);
        int dWij = s.coCount(w1, w2);

        if (dWj == 0) return Double.NaN;
        return Math.log((dWij + EPSILON) / (double) dWj);
    }

    /* ======================================================================
     *  Budowanie statystyk i klasy pomocnicze
     * ====================================================================== */

    /**
     * Przetwarza listę dokumentów w celu zbudowania statystyk potrzebnych do obliczeń.
     */
    private WordCooccurrenceStats buildStats(List<String> topWords, List<String> docs) {
        Map<String, Integer> wordCounts = new HashMap<>();
        Map<String, Integer> coOccurrenceCounts = new HashMap<>();
        Set<String> topWordsSet = new HashSet<>(topWords);

        for (String doc : docs) {
            Set<String> presentWords = Arrays.stream(doc.toLowerCase().split("\\s+"))
                    .filter(topWordsSet::contains)
                    .collect(Collectors.toSet());

            // Aktualizuj liczniki wystąpień pojedynczych słów
            presentWords.forEach(word -> wordCounts.merge(word, 1, Integer::sum));

            // Aktualizuj liczniki współwystąpień par słów
            List<String> wordsInDoc = new ArrayList<>(presentWords);
            for (int i = 0; i < wordsInDoc.size() - 1; i++) {
                for (int j = i + 1; j < wordsInDoc.size(); j++) {
                    String w1 = wordsInDoc.get(i);
                    String w2 = wordsInDoc.get(j);
                    // Klucz jest posortowany alfabetycznie dla spójności
                    String key = w1.compareTo(w2) < 0 ? w1 + "|" + w2 : w2 + "|" + w1;
                    coOccurrenceCounts.merge(key, 1, Integer::sum);
                }
            }
        }
        return new WordCooccurrenceStats(wordCounts, coOccurrenceCounts, docs.size());
    }

    /**
     * Funkcyjny interfejs dla metod obliczających wynik dla pary słów.
     */
    @FunctionalInterface
    private interface ScoreFunction {
        double apply(String w1, String w2);
    }

    /**
     * Wewnętrzna klasa pomocnicza do przechowywania i udostępniania statystyk.
     */
    static class WordCooccurrenceStats {
        private final Map<String, Integer> wordCounts;
        private final Map<String, Integer> coCounts;
        final int totalDocuments;

        WordCooccurrenceStats(Map<String, Integer> wordCounts,
                              Map<String, Integer> coCounts,
                              int totalDocuments) {
            this.wordCounts = wordCounts;
            this.coCounts = coCounts;
            this.totalDocuments = totalDocuments;
        }

        double prob(String word) {
            return (double) count(word) / totalDocuments;
        }

        /** Prawdopodobieństwo współwystąpienia pary słów w dokumencie. */
        double coProb(String w1, String w2) {
            return (double) coCount(w1, w2) / totalDocuments;
        }

        /** Liczba dokumentów zawierających słowo. */
        int count(String word) {
            return wordCounts.getOrDefault(word, 0);
        }

        /** Liczba dokumentów zawierających oba słowa. */
        int coCount(String w1, String w2) {
            String key = w1.compareTo(w2) < 0 ? w1 + "|" + w2 : w2 + "|" + w1;
            return coCounts.getOrDefault(key, 0);
        }
    }
}
