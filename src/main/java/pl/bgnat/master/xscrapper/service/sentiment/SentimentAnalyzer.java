package pl.bgnat.master.xscrapper.service.sentiment;

import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.config.sentiment.SentimentProperties;
import pl.bgnat.master.xscrapper.model.sentiment.SentimentLabel;
import pl.bgnat.master.xscrapper.utils.PolishStemmerUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SentimentAnalyzer {

    private static final SimpleTokenizer TOK = SimpleTokenizer.INSTANCE;
    private static final Set<String> NEGATIONS =
            Set.of("nie", "nigdy", "żaden", "bez", "ani");
    private static final Set<String> BOOSTERS =
            Set.of("bardzo", "mega", "super", "najbardziej");
    private static final Set<String> DIMINISHERS =
            Set.of("trochę", "lekko", "nieco");

    private static final Map<String, Double> EMOJI =
            Map.of("🙂", 1.5, "😊", 2.0, "❤️", 3.0,
                    "😢", -2.0, "😡", -3.0, "💔", -2.5);

    private final LexiconLoader lex;
    private final double posT, negT;   // progi
    private final Map<String, String> lemmaCache = new ConcurrentHashMap<>();

    public SentimentAnalyzer(LexiconLoader loader, SentimentProperties sentimentProperties) {
        this.lex = loader;
        this.posT = sentimentProperties.getPositiveThreshold();
        this.negT = sentimentProperties.getNegativeThreshold();
    }

    /* ------------------- API ------------------- */

    public double computeScore(String text) {
        if (text == null || text.isBlank()) return 0.0;

        String[] raw = TOK.tokenize(preprocessEmoji(text));

        double sum = 0;
        int tokenCount = 0;
        boolean negateWindow = false;
        int window = 0;

        for (String tok : raw) {
            String lower = tok.toLowerCase();

            /* 1. emoji ↔ wartość */
            if (EMOJI.containsKey(tok)) {
                sum += EMOJI.get(tok);
                tokenCount++;
                continue;
            }

            /* 2. negacja otwiera 3-tokenowe okno inwersji */
            if (NEGATIONS.contains(lower)) {
                negateWindow = true;
                window = 3;
                continue;
            }

            /* 3. lematyzacja + słownik */
            String lemma = lemmaCache.computeIfAbsent(lower, PolishStemmerUtil::lemmatize);
            double val = lex.polarity(lemma);
            if (val == 0.0) continue;           // brak w słowniku → pomijamy

            /* 4. booster / diminisher przed bieżącym słowem? */
            double factor = 1.0;
            if (BOOSTERS.contains(lemma)) factor = 1.5;
            if (DIMINISHERS.contains(lemma)) factor = 0.5;

            /* 5. negacja inwersji */
            if (negateWindow) val = -val;

            sum += val * factor;
            tokenCount++;

            /* zamykamy okno negacji */
            if (negateWindow) {
                window--;
                if (window == 0) negateWindow = false;
            }
        }
        return tokenCount == 0 ? 0.0 : sum / tokenCount;   // średnia zamiast sumy
    }

    public SentimentLabel classify(double score) {
        return score >= posT ? SentimentLabel.POSITIVE
                : score <= negT ? SentimentLabel.NEGATIVE
                : SentimentLabel.NEUTRAL;
    }

    /* ------------------- utils ------------------- */

    private String preprocessEmoji(String txt) {
        return txt.replace(":)", "🙂")
                .replace(":-)", "🙂")
                .replace(":(", "😢")
                .replace(":-(", "😢")
                .replace("<3", "❤️");
    }
}
