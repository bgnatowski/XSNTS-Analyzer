package pl.bgnat.master.xscrapper.utils;

import lombok.NoArgsConstructor;
import morfologik.stemming.WordData;
import morfologik.stemming.polish.PolishStemmer;

import java.util.List;

@NoArgsConstructor
public final class PolishStemmerUtil {

    private static final ThreadLocal<PolishStemmer> TL_STEMMER =
            ThreadLocal.withInitial(PolishStemmer::new);


    public static String lemmatize(String token) {
        List<WordData> data = TL_STEMMER.get().lookup(token.toLowerCase());
        return data.isEmpty() ? token.toLowerCase()
                : data.get(0).getStem().toString();
    }
}
