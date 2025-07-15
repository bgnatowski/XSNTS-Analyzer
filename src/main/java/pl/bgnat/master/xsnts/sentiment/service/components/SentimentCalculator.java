package pl.bgnat.master.xsnts.sentiment.service.components;

import pl.bgnat.master.xsnts.sentiment.dto.SentimentScore;

import java.util.Collection;

public interface SentimentCalculator {
    SentimentScore evaluate(Collection<String> tokens);
}
