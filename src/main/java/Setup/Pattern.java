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
        int thisLen = 0;
        for (int i = 0; i < this.natLangKey.length; i+=2) {
            thisLen += this.natLangKey[i].length();
        }

        int otherLen = 0;
        for (int i = 0; i < other.natLangKey.length; i+=2) {
            otherLen += other.natLangKey[i].length();
        }

        return -Integer.compare(thisLen, otherLen);
    }
}
