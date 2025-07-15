package pl.bgnat.master.xsnts.topicmodeling.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO przechowujące obliczone metryki spójności.
 */
@Data
@Builder
public class CoherenceMetrics {
    private Double pmi;
    private Double npmi;
    private Double umass;
    private Double uci;
    private Double perplexity;
    private Integer wordCount;
    private Integer documentCount;

    public static CoherenceMetrics empty() {
        return CoherenceMetrics.builder()
                .pmi(Double.NaN).npmi(Double.NaN).umass(Double.NaN).uci(Double.NaN).perplexity(Double.NaN)
                .wordCount(0).documentCount(0).build();
    }

    /**
     * Zwraca tekstową interpretację wyniku spójności PMI (Pointwise Mutual Information).
     * Interpretacja bazuje na ogólnie przyjętych progach w literaturze NLP.
     *
     * @return String z czytelną interpretacją wyniku PMI.
     */
    public String getPmiInterpretation() {
        if (pmi == null) {
            return "Brak danych do interpretacji wyniku PMI.";
        }

        if (pmi > 0.5) {
            return "Bardzo wysoka spójność (PMI > 0.5): Słowa w temacie są silnie skorelowane i często występują razem. Temat jest bardzo dobrze zdefiniowany.";
        } else if (pmi > 0.1) {
            return "Dobra spójność (PMI > 0.1): Słowa w temacie wykazują znaczącą korelację semantyczną. Temat jest dobrze zdefiniowany.";
        } else if (pmi > 0.0) {
            return "Akceptowalna spójność (PMI > 0.0): Słowa występują razem częściej niż losowo, co wskazuje na pewną spójność tematyczną.";
        } else if (pmi == 0.0) {
            return "Brak korelacji (PMI = 0.0): Słowa są statystycznie niezależne. Temat może być słabo zdefiniowany.";
        } else {
            return "Niska spójność (PMI < 0.0): Słowa w temacie występują razem rzadziej niż losowo. Temat jest prawdopodobnie niespójny lub jest mieszaniną różnych koncepcji.";
        }
    }

    /**
     * Zwraca tekstową interpretację wyniku spójności UMass.
     * Interpretacja opiera się na skali logarytmicznej, gdzie wartości bliższe zeru są lepsze.
     *
     * @return String z czytelną interpretacją wyniku UMass.
     */
    public String getUmassInterpretation() {
        if (umass == null) {
            return "Brak danych do interpretacji wyniku UMass.";
        }

        if (umass > -2.0) {
            return "Bardzo wysoka spójność (UMass > -2.0): Słowa w temacie bardzo często współwystępują w tych samych dokumentach. Temat jest wyjątkowo spójny.";
        } else if (umass > -5.0) {
            return "Dobra spójność (UMass > -5.0): Słowa w temacie wykazują silną tendencję do współwystępowania. Temat jest dobrze zdefiniowany.";
        } else if (umass > -10.0) {
            return "Akceptowalna spójność (UMass > -10.0): Słowa w temacie mają pewną tendencję do współwystępowania, temat jest umiarkowanie spójny.";
        } else {
            return "Niska spójność (UMass < -10.0): Słowa w temacie rzadko występują razem. Temat jest prawdopodobnie niespójny lub losowy.";
        }
    }
}
