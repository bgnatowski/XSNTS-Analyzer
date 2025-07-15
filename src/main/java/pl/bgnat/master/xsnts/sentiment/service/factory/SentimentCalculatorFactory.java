package pl.bgnat.master.xsnts.sentiment.service.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;

@Component
@RequiredArgsConstructor
public class SentimentCalculatorFactory {

    private final LexiconCalculator lexiconCalc;
    private final HfApiCalculator   hfCalc;

    public SentimentCalculator choose(SentimentStrategyLabel strategy) {
        return switch (strategy) {
            case STANDARD -> lexiconCalc;
            case HF_API -> hfCalc;
        };
    }
}

