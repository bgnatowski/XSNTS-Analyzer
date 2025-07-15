package pl.bgnat.master.xsnts.normalization.dto;

public enum TokenStrategyLabel {
    // "normal" lub "lemmatized" - albo , albo po normalizacji
    NORMAL, // bierze do analizy tokeny normalne (tj processedTweet.tokens)
    LEMMATIZED // bierze do analizy tokeny zlemmatyzowane (tj. processedTweet.tokensLemmatized)
}
