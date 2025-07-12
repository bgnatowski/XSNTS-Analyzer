package pl.bgnat.master.xsnts.normalization.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import morfologik.stemming.WordData;
import morfologik.stemming.polish.PolishStemmer;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextStemmerPL {

    private static final ThreadLocal<PolishStemmer> TL_STEMMER = ThreadLocal.withInitial(PolishStemmer::new);

    public static String lemmatize(String token) {
        if (token.startsWith("#")) {
            String hashtag = token.substring(1);

            // Jeśli hashtag zawiera cyfry lub jest dłuższy niż jedno słowo, zostawiamy bez zmian
            if (hashtag.matches(".*\\d.*") || hashtag.contains("_") || isCamelCase(hashtag)) {
                return token;
            }

            // Lematyzacja prostego słowa po #
            List<WordData> data = TL_STEMMER.get().lookup(hashtag);
            String lemma = data.isEmpty() ? hashtag.toLowerCase() : data.get(0).getStem().toString();
            return "#" + lemma;
        } else {
            List<WordData> data = TL_STEMMER.get().lookup(token);
            return data.isEmpty() ? token.toLowerCase() : data.get(0).getStem().toString();
        }
    }

    private static boolean isCamelCase(String s) {
        return s.matches(".*[a-z][A-Z].*");
    }

}
