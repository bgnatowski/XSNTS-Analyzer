package pl.bgnat.master.xsnts.exporter.service.exporters;

import pl.bgnat.master.xsnts.normalization.dto.TokenStrategyLabel;
import pl.bgnat.master.xsnts.sentiment.dto.SentimentStrategyLabel;

public interface Exporter {
    default String export(String userPath) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }
    default String export(Long id, String userPath) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }

    default String export(TokenStrategyLabel tokenStrategy,
           SentimentStrategyLabel sentimentModelStrategy,
           String userPath) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }
}

