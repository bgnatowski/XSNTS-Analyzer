package pl.bgnat.master.xscrapper.dto.normalization;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO przechowujące wyniki procesu normalizacji i tokenizacji tekstu.
 */
@Data
@Builder
public class NormalizedTweet {
    private String normalizedContent;
    private List<String> tokens;
    private List<String> tokensLemmatized;
    private Integer tokenCount;
}
