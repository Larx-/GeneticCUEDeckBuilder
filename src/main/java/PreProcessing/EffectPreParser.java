package PreProcessing;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
public class EffectPreParser {

    public static void main(String[] args) throws IOException {
        RegexPreProcessor regexPreProcessor = new RegexPreProcessor();
        String preFormatted = regexPreProcessor.processCardList();
        String[] formattedRows = preFormatted.split("\n");
        List<String> failedChunks = new ArrayList<>();
        List<String> chunkSequences = new ArrayList<>();
        int failedRowNumber = 0;

        String lastFailed = "";
        boolean chunkName = false;
        for (String row : formattedRows) {
            String effect = row.substring(row.lastIndexOf("\t")+1);
            if (!effect.equalsIgnoreCase("NULL")) {

                String chunkSequence = "";
                int openedBrackets = 0;

                for (int i = 0; i < effect.length(); i++) {
                    char nextChar = effect.charAt(i);

                    if (openedBrackets == 1 && chunkName) {
                        if (nextChar == ':') {
                            chunkName = false;
                            if (!chunkSequence.equals("-!-")) {
                                chunkSequence += " -> ";
                            }
                        } else {
                            if (!chunkSequence.equals("-!-")) {
                                chunkSequence += nextChar;
                            }
                        }
                    }

                    if (nextChar == '[') {
                        chunkName = true;
                        openedBrackets++;

                    } else if (nextChar == ']') {
                        openedBrackets--;

                    } else {
                        if (openedBrackets <= 0 && nextChar != ' ') {
                            int newIndex = Math.min(effect.length(), effect.indexOf('[', i));
                            newIndex = newIndex < 0 ? effect.length() : newIndex;
                            failedChunks.add(effect.substring(i, newIndex));
                            i = newIndex-1;

                            chunkSequence = "-!-";

                            String newFailed = "PreParsing failed for: " + StringUtils.rightPad(row.substring(0,row.indexOf("\t")),10) + effect;
                            if (!newFailed.equals(lastFailed)) {
                                lastFailed = newFailed;
                                failedRowNumber++;
//                                log.debug(lastFailed);
                            }
                        }
                    }
                }
                if (!chunkSequence.equals("") && !chunkSequence.equals("-!-")) {
                    chunkSequence = chunkSequence.replace("[","");
                    chunkSequence = chunkSequence.replace("]","");
                    if (!chunkSequences.contains(chunkSequence)) {
                        chunkSequences.add(chunkSequence);
                    }
                }
            }
        }
        // <DEBUG>
        sortAndLogArray(failedChunks, "PreParsing failed for -> " + failedChunks.size() + " <- chunks in -> " + failedRowNumber + " <- rows ...");
        sortAndLogArray(chunkSequences, "Different chunk sequences: ");
        // </DEBUG>
    }

    public static void sortAndLogArray(List<String> array, String description) {
        Collections.sort(array);
        log.debug("");
        log.debug(description);
        log.debug("");

        for (String str : array) {
            log.debug("\""+str+"\"");
        }
        log.debug("");
    }
}
