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
        int failedRowNumber = 0;

        String lastFailed = "";
        for (String row : formattedRows) {
            String effect = row.substring(row.lastIndexOf("\t")+1);

            if (!effect.equalsIgnoreCase("NULL")) {
                int openedBrackets = 0;

                for (int i = 0; i < effect.length(); i++) {
                    if (effect.charAt(i) == '[') {
                        openedBrackets++;
                    } else if (effect.charAt(i) == ']') {
                        openedBrackets--;
                    } else {
                        if (openedBrackets <= 0 && effect.charAt(i) != ' ') {
                            int newIndex = Math.min(effect.length(), effect.indexOf('[', i));
                            newIndex = newIndex < 0 ? effect.length() : newIndex;
                            failedChunks.add(effect.substring(i, newIndex));
                            i = newIndex-1;

                            String newFailed = "PreParsing failed for: " + StringUtils.rightPad(row.substring(0,row.indexOf("\t")),10) + effect;
                            if (!newFailed.equals(lastFailed)) {
                                lastFailed = newFailed;
                                failedRowNumber++;
                                log.debug(lastFailed);
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(failedChunks);
        log.debug("");
        log.debug("PreParsing failed for -> " + failedChunks.size() + " <- chunks in -> " + failedRowNumber + " <- rows ...");
        log.debug("");

        for (String fC : failedChunks) {
            log.debug("\""+fC+"\"");
        }
    }
}
