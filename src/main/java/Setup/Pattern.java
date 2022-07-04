package Setup;

import lombok.Getter;

import java.util.Arrays;

public class Pattern implements Comparable<Pattern> {
    @Getter String[] natLangKey;
    @Getter String   jsonOutput;

    public Pattern(String[] natLangKey, String jsonOutput) {
        this.natLangKey = natLangKey;
        this.jsonOutput = jsonOutput;
    }

    @Override
    public int compareTo(Pattern other) {
        return -Integer.compare(this.natLangKey[0].length(), other.natLangKey[0].length());
    }
}
