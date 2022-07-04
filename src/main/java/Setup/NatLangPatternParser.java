package Setup;

import Enums.Album;
import Enums.Collection;
import lombok.Getter;

import java.util.*;

public class NatLangPatternParser {

    Map<String[],String> map = new HashMap<>();
    Set<String> cardNames;
    @Getter int missingEffectPatterns = 0;

    public NatLangPatternParser(Set<String> cardNames) {
        this.cardNames = cardNames;
        this.addPatterns();
    }

    public int getNumPatterns() {
        return map.size();
    }

    public String parseEffect(String naturalEffectString) throws Exception {
        String returnString = null;
        boolean foundPattern = false;

        for (String[] pattern : map.keySet()) {
            if (naturalEffectString.startsWith(pattern[0])) {

                foundPattern = true;
                for (int i = 2; i < pattern.length; i+=2) {
                    if(!naturalEffectString.contains(pattern[i])){
                        foundPattern = false;
                        break;
                    }
                }

                if (foundPattern) {
                    returnString = map.get(pattern);

                    for (int i = 1; i < pattern.length; i += 2) {
                        String[] replace = pattern[i].split("~");
                        String toReplace = naturalEffectString.substring(naturalEffectString.indexOf(pattern[i - 1]) + pattern[i - 1].length(), naturalEffectString.indexOf(pattern[i + 1]));

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

                        returnString = returnString.replaceAll("~" + replace[2] + "~",toReplace);
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
            this.missingEffectPatterns++;
            throw new Exception("Pattern '"+naturalEffectString+"' not found!");
        }
    }

    private void addPatterns() {
        try {
            this.addPattern(new String[]{"When returned to your deck, your Opponent's cards left in hand cost ","~NUMBER~1~"," more Energy next turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'RETURN'," +
                            "'Target':{'Who':'OPPONENT','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'ENERGY','Value':'+~1~'}," +
                            "'Duration':{'Type':'TIMER','Params':{'Value':'1'}}" +
                            "}]}");

            this.addPattern(new String[]{"When played, all cards have ","~NUMBER~1~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'" +
                            "}]}");

            this.addPattern(new String[]{"When played, ","~CARD_NAME~1~"," cards gain +","~NUMBER~2~"," Power until played."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
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

            this.addPattern(new String[]{"When played, if you have played ","~CARD_NAME~1~",", give ","~CARD_NAME~2~"," and ","~CARD_NAME~3~"," (wherever they are) +","~NUMBER~4~"," Power until played."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
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

            this.addPattern(new String[]{"When drawn, Lock a random card in your opponent's hand for this turn. If you are losing the round, also give it -","~NUMBER~1~"," Power until it is played."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'DRAW'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM'}," +
                            "'Effect':{'Type':'LOCK'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime': 'DRAW'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM'}," + // FIXME: Currently not the same random card
                            "'Effect':{'Type':'POWER','Value':'-~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Params':{'Value':'Loosing'}}]" +
                            "}]}");

            this.addPattern(new String[]{"When played, if you are winning the round, this card has ","~NUMBER~1~"," Power."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Params':{'Value':'Winning'}}]" +
                            "}]}");

            this.addPattern(new String[]{"When played, all ","~ALBUM~1~"," cards have ","~NUMBER~2~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'ALBUM','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]}");

            this.addPattern(new String[]{"When drawn, reduce the energy cost of ","~COLLECTION~1~"," in your hand by ","~NUMBER~2~"," for the rest of the game."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'COLLECTION','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]}");
            this.addPattern(new String[]{"When returned to your deck, if you are losing the round, gain ","~NUMBER~1~"," Energy."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'RETURN'," +
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
        if (this.map.containsKey(pattern)) {
            throw new Exception("Pattern already exists!" + Arrays.toString(pattern));
        }
        this.map.put(pattern, jsonString);
    }
}
