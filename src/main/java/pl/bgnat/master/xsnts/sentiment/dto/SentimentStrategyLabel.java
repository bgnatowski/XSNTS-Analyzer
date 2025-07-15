package pl.bgnat.master.xsnts.sentiment.dto;

public enum SentimentStrategyLabel {
    STANDARD, // na podstawie resources/sentiment/polish_lexicon.tsv w formacie <słowo> <double_score>
    HF_API // https://huggingface.co/tabularisai/multilingual-sentiment-analysis
}
