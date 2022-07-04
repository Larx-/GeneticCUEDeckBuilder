package Setup;

import Enums.Album;
import Enums.Collection;
import lombok.Getter;

import java.util.*;

public class NatLangPatternParser {

    List<Pattern> patterns = new ArrayList<>();
    Set<String> cardNames;
    @Getter int numEffectsWithoutPattern = 0;
    @Getter int numPatterns = 0;

    public NatLangPatternParser(Set<String> cardNames) {
        this.cardNames = cardNames;
        this.addPatterns();

        // Sort based on length
        Collections.sort(this.patterns);

        this.expandPatterns();
    }

    public String parseEffect(String naturalEffectString) throws Exception {
        String returnString = null;
        boolean foundPattern = false;

        for (Pattern pattern : this.patterns) {
            String[] patternKey = pattern.getNatLangKey();

            if (naturalEffectString.startsWith(patternKey[0])) {
                foundPattern = true;
                for (int i = 2; i < patternKey.length; i+=2) {
                    if(!naturalEffectString.contains(patternKey[i])){
                        foundPattern = false;
                        break;
                    }
                }

                if (foundPattern) {
                    returnString = pattern.getJsonOutput();

                    for (int i = 1; i < patternKey.length; i += 2) {
                        String[] replace = patternKey[i].split("~");
                        int endBeforeReplacement = naturalEffectString.indexOf(patternKey[i-1]) + patternKey[i-1].length();
                        int startAfterReplacement = naturalEffectString.indexOf(patternKey[i+1]);
                        String toReplace = naturalEffectString.substring(endBeforeReplacement, startAfterReplacement);

                        // Sanity check
                        switch (replace[1]) {
                            case "NUMBER":
                                int integer = Integer.parseInt(toReplace);
                                break;
                            case "COLLECTION":
                                if (Collection.fromString(toReplace) == null) {
                                    throw new Exception("Collection "+toReplace+" to replace not found!");
                                }
                                break;
                            case "ALBUM":
                                if (Album.fromString(toReplace) == null) {
                                    throw new Exception("Album "+toReplace+" to replace not found!");
                                }
                                break;
                            case "CARD_NAME":
                                if (!cardNames.contains(toReplace)) {
                                    throw new Exception("Card "+toReplace+" to replace not found!");
                                }
                                break;
                        }

                        returnString = returnString.replaceAll("~" + replace[2] + "~", toReplace);
                    }
                }
            }

            if (foundPattern) {
                break;
            }
        }

        if (foundPattern) {
            return returnString;

        } else {
            this.numEffectsWithoutPattern++;
            throw new Exception("Pattern '"+naturalEffectString+"' not found!");
        }
    }

    private void addPatterns() {
        try {
            this.addPattern(new String[]{"~TIME~ your Opponent's cards left in hand cost ","~NUMBER~1~"," more Energy next turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'OPPONENT','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'ENERGY','Value':'+~1~'}," +
                            "'Duration':{'Type':'TIMER','Params':{'Value':'1'}}" +
                            "}]}");

            this.addPattern(new String[]{"~TIME~ all cards have ","~NUMBER~1~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'" +
                            "}]}");

            this.addPattern(new String[]{"~TIME~ ","~CARD_NAME~1~"," cards gain +","~NUMBER~2~"," Power until played."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'UNTIL_PLAYED'" +
                            "}]}");

            this.addPattern(new String[]{"When played with ","~CARD_NAME~1~",", give that card +","~NUMBER~2~"," Power."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'NAME','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Params':{'Who':'SELF','What':'NAME','CompareTo':'~1~'}}]" +
                            "}]}");

            this.addPattern(new String[]{"~TIME~ if you have played ","~CARD_NAME~1~",", give ","~CARD_NAME~2~"," and ","~CARD_NAME~3~"," (wherever they are) +","~NUMBER~4~"," Power until played."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Params':{'Who':'SELF','What':'NAME','CompareTo':'~1~'}}]" +
                            "},{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Params':{'Who':'SELF','What':'NAME','CompareTo':'~1~'}}]" +
                            "}]}");

            this.addPattern(new String[]{"~TIME~ Lock a random card in your opponent's hand for this turn. If you are losing the round, also give it -","~NUMBER~1~"," Power until it is played."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM'}," +
                            "'Effect':{'Type':'LOCK'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM'}," + // FIXME: Currently not the same random card
                            "'Effect':{'Type':'POWER','Value':'-~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Params':{'Value':'Loosing'}}]" +
                            "}]}");

            this.addPattern(new String[]{"~TIME~ if you are winning the round, this card has ","~NUMBER~1~"," Power."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Params':{'Value':'Winning'}}]" +
                            "}]}");

            this.addPattern(new String[]{"~TIME~ all ","~ALBUM~1~"," cards have ","~NUMBER~2~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'ALBUM','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]}");

            this.addPattern(new String[]{"~TIME~ reduce the energy cost of ","~COLLECTION~1~"," in your hand by ","~NUMBER~2~"," for the rest of the game."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'COLLECTION','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]}");
            
            this.addPattern(new String[]{"~TIME~ if you are losing the round, gain ","~NUMBER~1~"," Energy."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]}");

            this.addPattern(new String[]{"When played on the first turn of a round, this card has ","~NUMBER~1~"," Power."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Params':{'Value':'1'}}]" +
                            "}]}");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addPattern(String[] pattern, String jsonString) throws Exception {
        this.addPattern(this.patterns, pattern, jsonString);
        this.numPatterns++;
    }

    private void addPattern(List<Pattern> patternList, String[] pattern, String jsonString) throws Exception {
        for (Pattern p : patternList) {
            if (Arrays.equals(p.getNatLangKey(), pattern)) {
                throw new Exception("Pattern already exists!" + Arrays.toString(pattern));
            }
        }
        patternList.add(new Pattern(pattern, jsonString));
    }

    private void expandPatterns() {
        List<Pattern> mapExpanded = new ArrayList<>();

        for (Pattern p : this.patterns) {
            String[] key = p.getNatLangKey();
            String value = p.getJsonOutput();

            if (key[0].contains("~TIME~")) {
                String[] keyDraw = key.clone();
                keyDraw[0] = key[0].replace("~TIME~", "When drawn,");
                String valueDraw = value.replace("~TIME~", "DRAW");

                String[] keyPlay = key.clone();
                keyPlay[0] = key[0].replace("~TIME~", "When played,");
                String valuePlay = value.replace("~TIME~", "PLAY");

                String[] keyReturn = key.clone();
                keyReturn[0] = key[0].replace("~TIME~", "When returned to your deck,");
                String valueReturn = value.replace("~TIME~", "RETURN");

                try {
                    addPattern(mapExpanded, keyDraw, valueDraw);
                    addPattern(mapExpanded, keyPlay, valuePlay);
                    addPattern(mapExpanded, keyReturn, valueReturn);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    addPattern(mapExpanded, key, value);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        this.patterns = mapExpanded;
    }
}
