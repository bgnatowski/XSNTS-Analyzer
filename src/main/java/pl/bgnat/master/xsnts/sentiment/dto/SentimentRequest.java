package pl.bgnat.master.xsnts.sentiment.dto;

import lombok.Builder;
import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;

@Builder
public record SentimentRequest(
        TokenStrategyLabel tokenStrategy,
        SentimentStrategyLabel sentimentModelStrategy) {}

