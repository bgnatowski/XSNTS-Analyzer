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

    public static Pattern createNumberPattern() {
        return Pattern.compile("\\b\\d{5,}\\b");
    }

    public static Pattern createPunctuationPattern() {
        // usuwa znaki interpunkcyjne ale zostawia # i @
        return Pattern.compile("[^\\p{L}\\p{N}\\s#@]");
    }

    public static Pattern createWhitespacePattern() {
        return Pattern.compile("\\s+");
    }
}
