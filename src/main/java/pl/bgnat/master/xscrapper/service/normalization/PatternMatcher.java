package pl.bgnat.master.xscrapper.service.normalization;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Klasa odpowiedzialna za tworzenie wzorców regex.
 */
@Slf4j
@Component
public class PatternMatcher {

    public Pattern createUrlPattern() {
        return Pattern.compile("https?://\\S+|www\\.\\S+");
    }

    public Pattern createMentionPattern() {
        return Pattern.compile("@\\w+");
    }

    public Pattern createHashtagPattern() {
        return Pattern.compile("#\\w+");
    }

    public Pattern createNumberPattern() {
        return Pattern.compile("\\d+");
    }

    public Pattern createPunctuationPattern() {
        return Pattern.compile("[^\\p{L}\\p{N}\\s]");
    }

    public Pattern createWhitespacePattern() {
        return Pattern.compile("\\s+");
    }

    public Pattern createEmoticonsPattern() {
        // Podstawowe emotikony tekstowe
        return Pattern.compile("[:;=][-]?[)\\]}>DPp]|[)\\]}>DPp]|[\\(\\[<{][-]?[:;=]");
    }
}
