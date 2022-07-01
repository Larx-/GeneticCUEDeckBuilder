package Setup;

import Enums.Album;
import Enums.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NatLangPatternParser {

    public static void main(String[] args) throws Exception {
        NatLangPatternParser parser = new NatLangPatternParser(new HashSet<>());
        System.out.println(parser.parseEffect("When returned to your deck, your Opponent's cards left in hand cost 3 more Energy next turn."));
    }

    Map<String[],String> map = new HashMap<>();
    Set<String> cardNames;

    public NatLangPatternParser(Set<String> cardNames) {
        this.cardNames = cardNames;

        String[] pattern1 = new String[]{"When returned to your deck, your Opponent's cards left in hand cost ","~NUMBER~1~"," more Energy next turn."};
        String effect1 = "{'Effects': [{" +
                        "'TriggerTime': 'RETURN'," +
                        "'Target':{'Who':'OPPONENT','Where':'CARDS_REMAINING'}," +
                        "'Effect':{'Type':'ENERGY','Params':{'Value':'+~1~'}}," +
                        "'Duration':{'Type':'TIMER','Params':{'Value':'1'}}" +
                        "}]}";
        map.put(pattern1,effect1);
        String[] pattern2 = new String[]{"When played, all cards have ","~NUMBER~1~"," Power this turn."};
        String effect2 = "{'Effects': [{" +
                        "'TriggerTime': 'PLAY'," +
                        "'Target':{'Who':'BOTH','Where':'CARDS_IN_HAND'}," +
                        "'Effect':{'Type':'POWER','Params':{'Value':'~1~'}}," +
                        "'Duration':'END_TURN'" +
                        "}]}";
        map.put(pattern2,effect2);
        String[] pattern3 = new String[]{"When played, ","~CARD_NAME~1~"," cards gain +","~NUMBER~2~"," Power until played."};
        String effect3 = "{'Effects': [{" +
                        "'TriggerTime': 'PLAY'," +
                        "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'~1~'}," +
                        "'Effect':{'Type':'POWER','Params':{'Value':'+~2~'}}," +
                        "'Duration':'RETURN'" + // FIXME: until played not implemented!?!
                        "}]}";
        map.put(pattern3,effect3);
    }


    public String parseEffect(String naturalEffectString) throws Exception {
        String returnString = null;

        for (String[] pattern : map.keySet()) {
            boolean foundPattern = false;
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

                        returnString = returnString.replace("~" + replace[2] + "~",toReplace);
                    }
                }
            }
            if (foundPattern) {
                break;
            }
        }

        System.out.println("TRANSLATED EFFECT: "+returnString);

        return returnString;
    }
}
