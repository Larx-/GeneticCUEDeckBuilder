package PreProcessing;

import Effects.Effect;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class EffectChunkParser {

    public static void main(String[] args) {
        RegexPreProcessor regexPreProcessor = new RegexPreProcessor();
        String processed = regexPreProcessor.readFullFile(RegexPreProcessor.regexFile);
        String[] rows = processed.split("\n");

        for (String row : rows) {
            String effectString = row.substring(row.lastIndexOf("\t")+1);
            Effect parseEffect = parseEffect(effectString.trim());
            log.debug(parseEffect);
        }
    }

    private static Effect parseEffect (String effect) {
        if (!effect.equalsIgnoreCase("NULL")) {

            int openedBrackets = 0;
            String chunkInBrackets = "";
            List<String> chunks = new ArrayList<>();

            for (int i = 0; i < effect.length(); i++) {

                char nextChar = effect.charAt(i);
                if (nextChar == '[') {
                    openedBrackets++;

                } else if (nextChar == ']') {
                    openedBrackets--;

                    if (openedBrackets == 0) {
                        chunks.add(chunkInBrackets);
                        chunkInBrackets = "";
                    }

                } else {
                    if (openedBrackets <= 0 && nextChar != ' ') {
                        log.error("Could not parse effect: " + effect);
                        return null; // Effect could not be parsed

                    } else {
                        chunkInBrackets += nextChar;
                    }
                }
            }

            return parseChunks(chunks);
        }

        return null; // Default, no effect
    }

    private static Effect parseChunks (List<String> chunks) {
        log.debug(chunks);
        return null; // Default, no effect
    }
}
