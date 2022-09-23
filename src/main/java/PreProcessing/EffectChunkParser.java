package PreProcessing;

import Effects.Effect;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j2
public class EffectChunkParser {

    Set<String> cardNames;

    public static void main(String[] args) {
        EffectChunkParser effectChunkParser = new EffectChunkParser();
        effectChunkParser.parse();
    }

    private void parse () {
        RegexPreProcessor regexPreProcessor = new RegexPreProcessor();
        String processed = regexPreProcessor.readFullFile(RegexPreProcessor.regexFile);
        String[] rows = processed.split("\n");

        this.cardNames = new HashSet<>();
        for (String row : rows) {
            String[] rowParts   = row.split("\t");
            String cardName     = rowParts[1];
            this.cardNames.add(cardName.toLowerCase().trim());
        }

        List<List<Effect>> effectListList = new ArrayList<>();
        for (String row : rows) {
            String[] rowParts   = row.split("\t");
            String cardName     = rowParts[1];
            String effectString = rowParts[7];

            List<Effect> parseEffect = parseEffect(cardName.trim(), effectString.trim());
            effectListList.add(parseEffect);
            //log.debug(parseEffect);
        }
        //log.debug(effectListList);
    }

    private List<Effect> parseEffect (String cardName, String effect) {
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
                        //log.error("Could not parse effect: " + effect);
                        return null; // Effect could not be parsed
                    } else {
                        if (!(chunkInBrackets.equals("") && nextChar == ' ')) {
                            chunkInBrackets += nextChar;
                        }
                    }
                }
            }
            return parseChunks(cardName, chunks);
        }
        return null; // Default, no effect
    }

    private List<Effect> parseChunks (String cardName, List<String> chunks) {
        EffectBuilder effectBuilder = new EffectBuilder(this.cardNames);

        while (!chunks.isEmpty()) {
            String nextChunk = chunks.remove(0);
            String chunkType  = nextChunk.substring(0,nextChunk.indexOf(":"));
            String chunkParam = nextChunk.substring(nextChunk.indexOf(":")+1);
            effectBuilder.addChunk(chunkType, chunkParam, cardName);
        }

        return effectBuilder.build();
    }
}
