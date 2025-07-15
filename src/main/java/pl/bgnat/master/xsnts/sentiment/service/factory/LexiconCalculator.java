package pl.bgnat.master.xsnts.sentiment.service.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentScore;
import pl.bgnat.master.xsnts.sentiment.service.components.LexiconProvider;

import java.util.Collection;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LexiconCalculator implements SentimentCalculator {

    @Value("${app.sentiment.threshold:0.3}")
    private double threshold;                     // próg dla klasyfikacji

    @Value("${app.sentiment.negation-window:3}")
    private int negationWindow;                   // słowa do odwrócenia po "nie"

    @Value("${app.sentiment.intensifier-factor:1.5}")
    private double INTENSIFIER_FACTOR;            // mnożnik intensyfikatora typu super, bardzo mega

    // proste listy słów negacji i intensyfikacji
    private static final Set<String> NEGATIONS = Set.of(
            "nie", "nigdy", "bez", "brak", "żaden", "ani"
    );
    private static final Set<String> INTENSIFIERS = Set.of(
            "bardzo", "mega", "super"
    );

    private final LexiconProvider lexicon;

    public SentimentScore evaluate(Collection<String> tokens) {

        double sum = 0;
        int    negateLeft = 0;     // 0 → brak aktywnej negacji
        double nextMultiplier = 1;    // 1 → brak aktywnego intensyfikatora

        for (String raw : tokens) {
            String t = raw.toLowerCase();

            if (NEGATIONS.contains(t)) {
                negateLeft = negationWindow;
                continue;                       // samo „nie” nie ma ładunku
            }

            if (INTENSIFIERS.contains(t)) {
                nextMultiplier = INTENSIFIER_FACTOR;
                continue;
            }

            double val = scoreWithPrefixNie(t);

            // odwrócenie po negacji
            if (negateLeft > 0) {
                val = -val;
                negateLeft--;
            }

            //  wzmocnienie po intensyfikatorze
            val *= nextMultiplier;
            nextMultiplier = 1;                 // reset mnożnika

            sum += val;
        }

        SentimentLabel label =
                sum >  threshold ? SentimentLabel.POSITIVE :
                        sum < -threshold ? SentimentLabel.NEGATIVE :
                                SentimentLabel.NEUTRAL;

        return new SentimentScore(label, sum); //suma byc dowolnie duza/mała to tylko liczba
    }

    /**  Prefiks "nie-" – odwraca znak, jeśli rdzeń istnieje w słowniku. np nieszczery*/
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
}
