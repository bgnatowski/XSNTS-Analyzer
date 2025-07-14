package pl.bgnat.master.xsnts.sentiment.service.components;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentLabel;

import java.util.Collection;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SentimentCalculator {

    /* ---------- KONFIGURACJA Z  application.yml  ---------- */
    @Value("${app.sentiment.threshold:0.25}")
    private double threshold;                     // próg klasyfikacji

    @Value("${app.sentiment.negation-window:3}")
    private int negationWindow;                   // słowa do odwrócenia po „nie”

    @Value("${app.sentiment.intensifier-factor:1.5}")
    private double INTENSIFIER_FACTOR;            // mnożnik intensyfikatora
    /* ------------------------------------------------------ */

    private static final Set<String> NEGATIONS = Set.of(
            "nie", "nigdy", "bez", "brak", "żaden", "ani"
    );
    private static final Set<String> INTENSIFIERS = Set.of(
            "bardzo", "mega", "super"
    );

    private final LexiconProvider lexicon;

    /* ======= PUBLICZNE API ================================================== */
    public SentimentScore evaluate(Collection<String> tokens) {

        double sum           = 0;
        int    negateLeft    = 0;     // 0 → brak aktywnej negacji
        double nextMultiplier = 1;    // 1 → brak aktywnego intensyfikatora

        for (String raw : tokens) {
            String t = raw.toLowerCase();

            /* -------- 1. klasyczne słowo negujące -------- */
            if (NEGATIONS.contains(t)) {
                negateLeft = negationWindow;
                continue;                       // samo „nie” nie ma ładunku
            }

            /* -------- 2. intensyfikator ----------------- */
            if (INTENSIFIERS.contains(t)) {
                nextMultiplier = INTENSIFIER_FACTOR;
                continue;
            }

            /* -------- 3. ładunek słownikowy ------------- */
            double val = scoreWithPrefixNie(t);

            /*    3a. odwrócenie po negacji */
            if (negateLeft > 0) {
                val = -val;
                negateLeft--;
            }

            /*    3b. wzmocnienie po intensyfikatorze      */
            val *= nextMultiplier;
            nextMultiplier = 1;                 // reset mnożnika

            sum += val;
        }

        SentimentLabel label =
                sum >  threshold ? SentimentLabel.POSITIVE :
                        sum < -threshold ? SentimentLabel.NEGATIVE :
                                SentimentLabel.NEUTRAL;

        return new SentimentScore(label, sum);
    }

    /* ======= PRYWATNE POMOCNICZE =========================================== */

    /**  Prefiks „nie-” – odwraca znak, jeśli rdzeń istnieje w słowniku. */
    private double scoreWithPrefixNie(String token) {
        if (token.startsWith("nie") && token.length() > 3) {
            String stem = token.substring(3);                // bez „nie”
            double stemVal = lexicon.score(stem);
            if (stemVal != 0) {
                return -stemVal;                             // odwrócony znak
            }
        }
        return lexicon.score(token);
    }

    /* ------------------------  RECORD -------------------------------------- */
    public record SentimentScore(SentimentLabel label, double value) {}
}
