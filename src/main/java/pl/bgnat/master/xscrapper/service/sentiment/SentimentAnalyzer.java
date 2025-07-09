package pl.bgnat.master.xscrapper.service.sentiment;

import lombok.RequiredArgsConstructor;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xscrapper.config.sentiment.SentimentProperties;
import pl.bgnat.master.xscrapper.model.sentiment.SentimentLabel;
import pl.bgnat.master.xscrapper.utils.PolishStemmerUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SentimentAnalyzer {

    private static final SimpleTokenizer TOK = SimpleTokenizer.INSTANCE;
    private final Map<String,String> lemmaCache = new ConcurrentHashMap<>();
    private final Map<String,Double> lexicon;
    private final double neg;
    private final double pos;

    @Autowired
    public SentimentAnalyzer(LexiconLoader loader, SentimentProperties sentimentProperties) {
        this.lexicon = loader.getLexicon();
        this.pos = sentimentProperties.getPositiveThreshold();
        this.neg = sentimentProperties.getNegativeThreshold();
    }

    public double computeScore(String text) {
        if (text == null || text.isBlank()) return 0;
        return Arrays.stream(TOK.tokenize(text))
                .map(t -> lemmaCache.computeIfAbsent(t.toLowerCase(), PolishStemmerUtil::lemmatize))
                .mapToDouble(t -> lexicon.getOrDefault(t, 0D))
                .sum();
    }

    public SentimentLabel classify(double score) {
        return score >= pos ? SentimentLabel.POSITIVE
                : score <= neg ? SentimentLabel.NEGATIVE
                : SentimentLabel.NEUTRAL;
    }
}

