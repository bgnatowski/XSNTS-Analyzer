package pl.bgnat.master.xsnts.sentiment.dto;

public enum SentimentStrategyLabel {
    STANDARD, // na podstawie resources/sentiment/polish_lexicon.tsv w formacie <słowo> <double_score>
    SVM_AND_BOW // korzysta z https://github.com/riomus/polish-sentiment/tree/master?tab=readme-ov-file
}
