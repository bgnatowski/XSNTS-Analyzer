package pl.bgnat.master.xsnts.sentiment.service.components;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentLabel;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class SentimentCalculator {

    @Value("${app.sentiment.threshold:0.1}")
    private double threshold;

    private final LexiconProvider lexicon;

    public SentimentScore evaluate(Collection<String> tokens) {
        double raw = tokens.stream()
                .mapToDouble(lexicon::score)
                .sum();

        SentimentLabel label = raw > threshold  ? SentimentLabel.POSITIVE
                : raw < -threshold ? SentimentLabel.NEGATIVE
                : SentimentLabel.NEUTRAL;

        return new SentimentScore(label, raw);
    }

    public record SentimentScore(SentimentLabel label, double value) {}
}
