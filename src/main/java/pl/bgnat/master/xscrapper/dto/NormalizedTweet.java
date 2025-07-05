package pl.bgnat.master.xscrapper.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO przechowujące wyniki procesu normalizacji i tokenizacji tekstu.
 */
@Data
@Builder
public class NormalizedTweet {

    /**
     * Znormalizowany tekst tweeta
     */
    private String normalizedContent;

    /**
     * Lista tokenów wyekstraktowanych z tekstu
     */
    private List<String> tokens;

    /**
     * Liczba tokenów
     */
    private Integer tokenCount;
}
