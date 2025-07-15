package pl.bgnat.master.xsnts.normalization.utils;

import java.util.regex.Pattern;

/**
 * Klasa odpowiedzialna za tworzenie wzorców regex dla normalizacji.
 */
public class PatternMatcher {

    public static Pattern createUrlPattern() {
        return Pattern.compile("https?://\\S+|www\\.\\S+");
    }

    public static  Pattern createMentionPattern() {
        return Pattern.compile("@\\w+");
    }

    public static  Pattern createHashtagPattern() {
        return Pattern.compile("#\\w+");
    }

    public static Pattern createShortNumberPattern() {
        String regex = "\\b\\d{1,3}\\b"; // liczby ale zostawia daty krotkie 1, 12, 234

        return Pattern.compile(regex);
    }

    public static Pattern createLongNumberPattern() {
        String regex = "\\b\\d{5,}\\b"; // liczby ale zostawia daty 20235 w zwyz

        return Pattern.compile(regex);
    }

    public static Pattern createPunctuationPattern() {
        // usuwa znaki interpunkcyjne ale zostawia # i @ oraz _
        return Pattern.compile("[^\\p{L}\\p{N}\\s#@_]");
    }

    public static Pattern createWhitespacePattern() {
        return Pattern.compile("\\s+");
    }

    public static Pattern createMultiHashPattern() {
        // ≥2 kolejnych # na początku tokenu – musi za nimi stać litera/cyfra/_
        return Pattern.compile("(?<=\\s|^)#{2,}(?=[\\p{L}\\p{N}_])");
    }

    public static Pattern createOrphanHashPattern() {
        // pojedynczy # otoczony spacjami lub końcem / początkiem wiersza
        return Pattern.compile("(?<=\\s|^)#+(?=\\s|$)");
    }


}
