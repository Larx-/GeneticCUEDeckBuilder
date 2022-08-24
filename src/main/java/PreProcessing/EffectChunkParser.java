package PreProcessing;

import Effects.Effect;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class EffectChunkParser {

    public static void main(String[] args) {
        EffectChunkParser effectChunkParser = new EffectChunkParser();
        effectChunkParser.parse();
    }

    private void parse () {
        RegexPreProcessor regexPreProcessor = new RegexPreProcessor();
        String processed = regexPreProcessor.readFullFile(RegexPreProcessor.regexFile);
        String[] rows = processed.split("\n");

        for (String row : rows) {
            String[] rowParts   = row.split("\t");
            String cardName     = rowParts[1];
            String effectString = rowParts[7];

            List<Effect> parseEffect = parseEffect(cardName.trim(), effectString.trim());
            log.debug(parseEffect);
        }
    }

    private List<Effect> parseEffect (String name, String effect) {
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
                        if (!(chunkInBrackets.equals("") && nextChar == ' ')) {
                            chunkInBrackets += nextChar;
                        }
                    }
                }
            }
            return parseChunks(name, chunks);
        }
        return null; // Default, no effect
    }

    private List<Effect> parseChunks (String name, List<String> chunks) {
        EffectBuilder effectBuilder = new EffectBuilder();

        // TODO: "Multi"-Effects
        while (!chunks.isEmpty()) {
            String nextChunk = chunks.remove(0);
            String chunkParam = nextChunk.substring(nextChunk.indexOf(":")+1);
            switch (nextChunk.substring(0,nextChunk.indexOf(":"))){
                case "TriggerTime":
                    effectBuilder.triggerTime(chunkParam);
                    break;
                case "Condition":
                    effectBuilder.condition(chunkParam);
                    break;
                case "Target":
                    effectBuilder.target(chunkParam, name);
                    break;
                case "Effect":
                    effectBuilder.effect(chunkParam);
                    break;
                case "Duration":
                    effectBuilder.duration(chunkParam);
                    break;
                default:
                    log.error("Parsing failed during chunk type determination.");
                    break;
            }
        }

        List<Effect> effectList = new ArrayList<>();
        effectList.add(effectBuilder.build());

        return effectList;
    }
}
