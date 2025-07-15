package pl.bgnat.master.xsnts.sentiment.service.components;

import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;

@FunctionalInterface
public interface TokenExtractor {
    String extract(ProcessedTweet tweet);

    static TokenExtractor of(TokenStrategyLabel strategy) {
        return switch (strategy) {
            case NORMAL     -> ProcessedTweet::getTokens;
            case LEMMATIZED -> ProcessedTweet::getTokensLemmatized;
        };
    }
}

