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

    int countDoubles = 0;

    public String parseEffect(String naturalEffectString, String cardname) throws Exception {
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
                    String choppedNatEffString = naturalEffectString;

                    for (int i = 1; i < patternKey.length; i += 2) {
                        String[] replace = patternKey[i].split("~");
                        int endBeforeReplacement = choppedNatEffString.indexOf(patternKey[i-1]) + patternKey[i-1].length();
                        int startAfterReplacement = choppedNatEffString.indexOf(patternKey[i+1], endBeforeReplacement);

                        if (startAfterReplacement < 0) {
                            foundPattern = false;
                            break;

                        } else {
                            String toReplace = choppedNatEffString.substring(endBeforeReplacement, startAfterReplacement);
                            choppedNatEffString = choppedNatEffString.substring(endBeforeReplacement);

                            // Sanity check
                            if (replace[1].equals("NUM")) {
                                int integer = Integer.parseInt(toReplace);

                            } else if (replace[1].equals("N_C")) { // NAME_CONTAINS
                                returnString = returnString.replaceAll("~N_C~", "NAME_CONTAINS");

                            } else { // COLLECTION, ALBUM or CARD_NAME = CAN
                                if (Album.fromString(toReplace) != null) {
                                    returnString = returnString.replaceAll("~CAN~", "ALBUM");

                                } else if (Collection.fromString(toReplace) != null) {
                                    returnString = returnString.replaceAll("~CAN~", "COLLECTION");

                                } else if (cardNames.contains(toReplace.toLowerCase())) {
                                    returnString = returnString.replaceAll("~CAN~", "NAME");

                                } else {
                                    // Filter out groupings of CANs, if the pattern does not explicitly describe them
                                    if (toReplace.contains(",") || toReplace.contains(" and ") || toReplace.contains(" or ")) {
                                        System.out.println(toReplace);
                                        System.out.println(++countDoubles);
                                        // Fixme: Is it worth writing a special case for "dissolving" the groupings,
                                        //  if in ~250 cards only ~18 would benefit from it
                                        //  or is it easier to just write all variations of the description
                                        //  (one or more collections as targets for example) as it's own pattern
                                        //  UPDATE:
                                        //  Out of 521 parsed effects, 64 run into this case and would need their own pattern
                                        //  Meaning for the last ~20% or so I'd have to rewrite patterns multiple times...
                                    }

                                    throw new Exception("Could not find Collection, Album or Card '"+toReplace+"' to replace!");
                                }
                            }
                            returnString = returnString.replaceAll("~" + replace[2] + "~", toReplace);
                        }
                    }

                    // Manually telling it what it is
                    returnString = returnString.replaceAll("~C~", "COLLECTION");
                    returnString = returnString.replaceAll("~A~", "ALBUM");
                    returnString = returnString.replaceAll("~N~", "NAME");

                    // Replace THIS with the card itself
                    returnString = returnString.replaceAll("'What':'THIS'", "'What':'NAME','CompareTo':'"+cardname+"'");
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

            String editString = naturalEffectString;
            if (editString.startsWith("When drawn,")) {
                editString = editString.replace("When drawn,", "~TIME~");
            } else if (editString.startsWith("When played,")) {
                editString = editString.replace("When played,", "~TIME~");
            } else if (editString.startsWith("When returned to your deck,")) {
                editString = editString.replace("When returned to your deck,", "~TIME~");
            }
            String triggerTime = editString.equals(naturalEffectString) ? "" : "~TIME~";

            String suggestion = "// (" + cardname + ") " + naturalEffectString + "\n" +
                            "this.addPattern(new String[]{\"" + editString + "\"},\n"+
                            "\"{'Effects':[{\" +\n" +
                            "\"'TriggerTime':'" + triggerTime + "',\" +\n" +
                            "\"'Target':{'Who':'','Where':'','What':'','CompareTo':''},\" +\n" +
                            "\"'Effect':{'Type':'','Value':''},\" +\n" +
                            "\"'Duration':'',\" +\n" +
                            "\"'Conditions':[{'Type':'','Who':'','Where':'','What':'','CompareTo':''}]\" +\n" +
                            "\"}],\" +\n" +
                            "\"'Combos':'[]'}\");";

            throw new Exception("Pattern '"+naturalEffectString+"' not found! Suggested pattern:\n"+suggestion);
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

    private void addPatterns() {
        try {
            this.addPattern(new String[]{"~TIME~ your Opponent's cards left in hand cost ","~NUM~1~"," more Energy next turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}" +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"~TIME~ all cards have ","~NUM~1~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'" +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"~TIME~ ","~CAN~1~"," cards gain ","~NUM~2~"," Power until played."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            this.addPattern(new String[]{"When played with ","~CAN~1~",", give that card +","~NUM~2~"," Power."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Who':'SELF','Where':'CARDS_PLAYED','What':'~CAN~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~",", give ","~CAN~2~"," and ","~CAN~3~"," (wherever they are) ","~NUM~4~"," Power until played."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "},{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~,~3~]'}");

            this.addPattern(new String[]{"~TIME~ Lock a random card in your Opponent's hand for this turn. If you are losing the round, also give it ","~NUM~1~"," Power until it is played."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented - Currently not the same random card
                            "'Effect':{'Type':'LOCK'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented - Currently not the same random card
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"~TIME~ if you are winning the round, this card has ","~NUM~1~"," Power."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"~TIME~ all ","~CAN~1~"," cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            this.addPattern(new String[]{"~TIME~ all your cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"~TIME~ reduce the energy cost of ","~CAN~1~"," in your hand by ","~NUM~2~"," for the rest of the game."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            this.addPattern(new String[]{"~TIME~ reduce the energy cost of ","~CAN~1~"," cards in your hand by ","~NUM~2~"," for the rest of the game."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            this.addPattern(new String[]{"~TIME~ if you are losing the round, gain ","~NUM~1~"," Energy."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"When played on the first turn of a round, this card has ","~NUM~1~"," Power."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"~TIME~ gain ","~NUM~1~"," Energy. If you won the turn, gain an extra ","~NUM~2~"," Energy."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime': 'RETURN'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"When this card returns to your deck, steal ","~NUM~1~"," Energy from your Opponent."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'RETURN'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime': 'RETURN'," +
                            "'Target':{'Who':'OTHER'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"When this returns to your deck, if you won this turn, gain ","~NUM~1~"," Power next turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'RETURN'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"~TIME~ if you are winning the round, you have ","~NUM~1~"," Power next turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            this.addPattern(new String[]{"~TIME~ if you won the turn, gain ","~NUM~1~"," Energy next turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~1~'}," +
                            "'Duration':{'Type':'PERMANENT'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // When drawn, if you have lost at least 1 round, give your Angela Maxwell's Walking the World cards (wherever they are) +15 Power this turn.
            this.addPattern(new String[]{"~TIME~ if you have lost at least ","~NUM~1~"," round, give your ","~CAN~2~"," cards (wherever they are) ","~NUM~3~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUNDS_LOST','Value':'>~1~'}]" +
                            "}]," +
                            "'Combos':'[~2~]'}");

            // When played, if you have lost two or more rounds, give your Angela Maxwells Walking the World (wherever they are) cards +14 Power permanently.
            this.addPattern(new String[]{"~TIME~ if you have lost two or more rounds, give your ","~CAN~1~"," (wherever they are) cards ","~NUM~2~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'ROUNDS_LOST','Value':'>~2~'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // While in your hand, at the start of each turn, if you are losing the round, give your Angela Maxwells Walking the World cards (wherever they are) +7 Power for 4 turns.
            this.addPattern(new String[]{"While in your hand, at the start of each turn, if you are losing the round, give your ","~CAN~1~"," cards (wherever they are) ","~NUM~2~"," Power for ","~NUM~3~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When returned to your deck, if you lost the turn, reduce the Energy cost of your Angela Maxwells Walking the World (wherever they are) cards by 2 for 3 turns.
            this.addPattern(new String[]{"~TIME~ if you lost the turn, reduce the Energy cost of your ","~CAN~1~"," (wherever they are) cards by ","~NUM~2~"," for ","~NUM~3~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When played, if you are losing the round, give your Angela Maxwells Walking the World (wherever they are) cards +25 Power for 3 turns.
            this.addPattern(new String[]{"~TIME~ if you are losing the round, give your ","~CAN~1~"," (wherever they are) cards ","~NUM~2~"," Power for ","~NUM~3~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When returned to your deck, if you lost the turn, reduce the Energy cost of your Angela Maxwells Walking the World cards (wherever they are) by 1 until played.
            this.addPattern(new String[]{"~TIME~ if you lost the turn, reduce the Energy cost of your ","~CAN~1~"," cards (wherever they are) by ","~NUM~2~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When drawn, if you are losing the round, give your Angela Maxwells Walking the World (wherever they are) cards +15 Power this turn.
            this.addPattern(new String[]{"~TIME~ if you are losing the round, give your ","~CAN~1~"," (wherever they are) cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When returned to your deck, if it is the first turn of the round, give your Web Surfers cards, wherever they are, +18 Power until played.
            this.addPattern(new String[]{"~TIME~ if it is the first turn of the round, give your ","~CAN~1~"," cards, wherever they are, ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When played, if your deck contains Shiba Inu, give your Dogs cards (wherever they are) +19 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your ","~CAN~2~"," cards (wherever they are) ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // When played, steal 2 Energy from your Opponent.
            this.addPattern(new String[]{"~TIME~ steal ","~NUM~1~"," Energy from your Opponent."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'~TIME~',"+
                            "'Target':{'Who':'OTHER'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            // When played, if your deck contains 9 or more Cute Cats, give them, wherever they are, +14 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~NUM~1~"," or more ","~CAN~2~",", give them, wherever they are, ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~','Value':'>=~1~'}]" +
                            "}]," +
                            "'Combos':'[~2~]'}");

            // When drawn, for each Solar System card in your deck, give the Earth card +8 Power.
            this.addPattern(new String[]{"~TIME~ for each ","~CAN~1~"," card in your deck, give the ","~CAN~2~"," card ","~NUM~3~"," Power."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~3~','CountEach':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','UpTo':'18','PlayHistory':'FALSE'}}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Humorism) When drawn, give a random card in your hand +50 Power this turn.nWhen played, give this card +20 Power permanently.nWhen returned to your deck, reduce the Energy cost of your Opponent's remaining cards by 2 for 2 turns.nWhile in your hand, at the start of each turn, give your cards +5 Power this turn.
            this.addPattern(new String[]{"When drawn, give a random card in your hand +50 Power this turn." +
                            "nWhen played, give this card +20 Power permanently." +
                            "nWhen returned to your deck, reduce the Energy cost of your Opponent's remaining cards by 2 for 2 turns." +
                            "nWhile in your hand, at the start of each turn, give your cards +5 Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented - Currently not the same random card
                            "'Effect':{'Type':'POWER','Value':'50'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND', 'What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'20'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-2'}," +
                            "'Duration':{'Type':'TIMER','Value':'2'}," +
                            "},{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'5'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Strawberry Moon) When played, your Curious Cuisine cards gain +20 Power this turn and next.
            this.addPattern(new String[]{"~TIME~ your ","~CAN~1~"," cards gain ","~NUM~2~"," Power this turn and next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Flower Moon) When played, give your Plant Life cards +20 Power this turn and next.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power this turn and next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Goodtimes Virus) When played, if it is the last turn of the round, give your History of the Internet cards, wherever they are, +14 Power until played.
            this.addPattern(new String[]{"~TIME~ if it is the last turn of the round, give your ","~CAN~1~"," cards, wherever they are, ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'3'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Friar Tuck) When drawn, for each Curious Cuisine card in your deck, (up to a maximum of four) give the Merry Men, even if they're in your deck, +8 Power until played.
            this.addPattern(new String[]{"When drawn, for each ","~CAN~1~"," card in your deck, (up to a maximum of four) give the Merry Men, even if they're in your deck, ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Friar Tuck'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~2~','CountEach':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','UpTo':'4','PlayHistory':'FALSE'}}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Will Scarlet'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~2~','CountEach':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','UpTo':'4','PlayHistory':'FALSE'}}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Little John'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~2~','CountEach':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','UpTo':'4','PlayHistory':'FALSE'}}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,Friar Tuck,Will Scarlet,Little John]'}");

            // (Robin Hood) When  played, give your Opponent's cards with 80 or more Base Power -20 Power this turn and give the Merry Men, even if they're in your deck, +20 Power permanently.
            this.addPattern(new String[]{"When  played, give your Opponent's cards with 80 or more Base Power -20 Power this turn and give the Merry Men, even if they're in your deck, +20 Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Friar Tuck'}," +
                            "'Effect':{'Type':'POWER','Value':'20'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Will Scarlet'}," +
                            "'Effect':{'Type':'POWER','Value':'20'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Little John'}," +
                            "'Effect':{'Type':'POWER','Value':'20'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'BASE_POWER','CompareTo':'>=80'}," +
                            "'Effect':{'Type':'POWER','Value':'-20'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[Friar Tuck,Will Scarlet,Little John]'}");

            // (Battle of Pelusium (525)) When played, if your deck contains Bastet, give your remaining cards in hand +15 Power permanently.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your remaining cards in hand ","~NUM~2~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Great Emu War) When played, for every Birds card in your deck, give your Opponent's cards -3 Power this turn.
            this.addPattern(new String[]{"~TIME~ for every ","~CAN~1~"," card in your deck, give your Opponent's cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~2~','CountEach':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','UpTo':'18','PlayHistory':'FALSE'}}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Battle of Leipzig (1813)) If played after turn 6 give your Opponents cards with 4 or less base energy wherever they are -18 power for 3 turns
            this.addPattern(new String[]{"If played after turn ","~NUM~1~"," give your Opponents cards with ","~NUM~2~"," or less base energy wherever they are ","~NUM~3~"," power for ","~NUM~4~"," turns"},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'BASE_ENERGY','CompareTo':'<=~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~4~'}," +
                            "'Conditions':[{'Type':'AFTER_TURN','Value':'~1~'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Battle of Waterloo (1815)) When returned to your deck, if you lost the turn, gain +120 Power/Turn next turn.
            this.addPattern(new String[]{"~TIME~ if you lost the turn, gain ","~NUM~1~"," Power/Turn next turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Siege of Baghdad (1258)) When drawn, your Opponent's Legendary cards, wherever they are, lose 25 Power for 3 turns.
            this.addPattern(new String[]{"~TIME~ your Opponent's Legendary cards, wherever they are, lose ","~NUM~1~"," Power for ","~NUM~2~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Lgnd'}," +
                            "'Effect':{'Type':'POWER','Value':'-~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~2~'}," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Mount Etna) While in your hand, this card Burns(10) until played. nWhen played, all other cards have -20 Power this turn.
            this.addPattern(new String[]{"While in your hand, this card Burns(","~NUM~1~",") until played. nWhen played, all other cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'BURN','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Piltdown Man) When played, if your deck contains The Brain, give your Primates and Human Evolution cards (even if they're in your deck) +12 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your ","~CAN~2~"," and ","~CAN~3~"," cards (even if they're in your deck) ","~NUM~4~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~,~3~]'}");

            // (Coffe) When returned to your deck, give this card -40 Power until played.
            this.addPattern(new String[]{"~TIME~ give this card ","~NUM~1~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Little John) When played, give all cards with a Base Energy of 3 or less -30 Power this turn. Repeat if played next to Robin Hood.
            this.addPattern(new String[]{"~TIME~ give all cards with a Base Energy of ","~NUM~1~"," or less ","~NUM~2~"," Power this turn. Repeat if played next to ","~CAN~3~","."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'BASE_ENERGY','CompareTo':'<=~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Who':'SELF','Where':'CARDS_PLAYED','What':'~N~','CompareTo':'~3~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'BASE_ENERGY','CompareTo':'<=~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Coterel Gang) When drawn, every time this card has been played this game, give your The Legend of Robin Hood cards +9 Power until played.
            this.addPattern(new String[]{"~TIME~ every time this card has been played this game, give your ","~CAN~1~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~2~','CountEach':" +
                            "{'Who':'SELF','Where':'CARDS_IN_DECK','What':'THIS','UpTo':'18','PlayHistory':'TRUE'}}," + // FIXME: PlayHistory should really be a What
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Prince John) At the start of each turn, while in your hand, give all The Legend of Robin Hood cards -3 Power.
            this.addPattern(new String[]{"At the start of each turn, while in your hand, give all ","~CAN~1~"," cards ","~NUM~2~"," Power."},
                    "{'Effects':[{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}"); // Never positive, so this would be an anti combo with ~1~

            // (Will Scarlet) When played on the first turn of the round, give your Robin Hood card, wherever it is, +73 Power until played.
            this.addPattern(new String[]{"When played on the first turn of the round, give your ","~CAN~1~"," card, wherever it is, ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Battle of Bach Dang (938)) When played, for each History card you have already played (up to a maximum 15), give your History cards +1 Power until played.
            this.addPattern(new String[]{"~TIME~ for each ","~CAN~1~"," card you have already played (up to a maximum ","~NUM~2~","), give your ","~CAN~3~"," cards ","~NUM~4~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~4~','CountEach':" +
                            "{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~','UpTo':'~2~','PlayHistory':'TRUE'}}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Neutrino) When returned to your deck, reduce the Energy cost of your remaining cards in hand by 3 until played.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your remaining cards in hand by ","~NUM~1~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // TODO: Can I somehow split combined effects like this on ".n" -> Probably not worth the time and trouble for ~40 such cards...
            // (D.B. Cooper) When drawn, lock this card in your hand for the rest of the round.nWhen played, for every Awesome Aviation card played this game by either player (up to a maximum of 18), give this card +10 Power this turn.nWhen returned to your deck, give your Money, Money, Money cards, wherever they are, +20 Power until played.
            this.addPattern(new String[]{"When drawn, lock this card in your hand for the rest of the round.n" +
                            "When played, for every ","~CAN~1~"," card played this game by either player (up to a maximum of 18), give this card +10 Power this turn.n" +
                            "When returned to your deck, give your ","~CAN~2~"," cards, wherever they are, +20 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'LOCK'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'10','CountEach':" +
                            "{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','UpTo':'18','PlayHistory':'TRUE'}}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'20'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Mary Toft"s Rabbit Birth) When returned to your deck, reduce the Energy cost of your Marvellous Medicine cards (even if they're in your deck) by 1 until played.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your ","~CAN~1~"," cards (even if they're in your deck) by ","~NUM~2~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-1'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Drop Bear) When played, if you have played Koala, your Carnivores cards (even if they're in your deck) gain +18 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~",", your ","~CAN~2~"," cards (even if they're in your deck) gain ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (The Donation of Constantine) When drawn, if your deck contains 3 or more The Roman Empire cards, give your cards with Alexandria in the name +25 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~NUM~1~"," or more ","~CAN~2~"," cards, give your cards with ","~N_C~3~"," in the name ","~NUM~4~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N_C~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~','Value':'>=~1~'}]" +
                            "}]," +
                            "'Combos':'[~2~,*~3~*]'}");

            // (Californian Runner Eggs) When played, give this card -20 Power for the rest of the game.
            this.addPattern(new String[]{"~TIME~ give this card ","~NUM~1~"," Power for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (E. Coli) When played in either the left or right slot, give your Science cards +8 Power permanently.
            this.addPattern(new String[]{"When played in either the left or right slot, give your ","~CAN~1~"," cards ","~NUM~2~"," Power permanently."}, // FIXME: Should I add a condition for this or is it unfair for the bot?
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Mycoplasma) When played, reduce the power of your Opponent's card opposite by 50, and reduce its energy cost by 2, for the rest of the game.
            this.addPattern(new String[]{"~TIME~ reduce the power of your Opponent's card opposite by 50, and reduce its energy cost by 2, for the rest of the game."},
                    "NULL"); // TODO: Opposite Target

            // (Triassic-Jurassic Extinction Event) When drawn, Lock this card in hand for 3 turns and give your Paleontology cards, wherever they are, +10 Power until played.
            this.addPattern(new String[]{"~TIME~ Lock this card in hand for ","~NUM~1~"," turns and give your ","~CAN~2~"," cards, wherever they are, ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'LOCK'}," +
                            "'Duration':{'Type':'TIMER','Value':'~1~'}," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~2~]'}");

            // (Pannotia) While in your hand, at the start of each turn, give this card -10 Power permanently.
            this.addPattern(new String[]{"While in your hand, at the start of each turn, give this card ","~NUM~1~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Great Dying) When returned to your deck, for every Paleontology card you have played this game, give your Paleontology cards (wherever they are) -10 Power permanently, and give your Life on Land and Oceans & Seas cards (wherever they are) +6 Power permanently.
            this.addPattern(new String[]{"~TIME~ for every Paleontology card you have played this game, give your Paleontology cards (wherever they are) -10 Power permanently, and give your Life on Land and Oceans & Seas cards (wherever they are) +6 Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Paleontology'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'-10','CountEach':" +
                            "{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Paleontology','UpTo':'1000','PlayHistory':'TRUE'}}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Life on Land'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'6','CountEach':" +
                            "{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Paleontology','UpTo':'1000','PlayHistory':'TRUE'}}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Oceans & Seas'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'6','CountEach':" +
                            "{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Paleontology','UpTo':'1000','PlayHistory':'TRUE'}}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[Oceans & Seas,Life on Land,Paleontology]'}");

            // (Lepidodendron) When drawn, give your Common and Rare Reptiles and Tremendous Trees cards (wherever they are) +20 Power permanently.
            this.addPattern(new String[]{"~TIME~ give your Common and Rare ","~CAN~1~"," and ","~CAN~2~"," cards (wherever they are) ","~NUM~3~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','Rarity':'Cmmn,Rare'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~','Rarity':'Cmmn,Rare'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Chicxulub Crater) When played, give all Paleontology cards -7 Power until played, and all Space cards +7 Power (wherever they are) until played.
            this.addPattern(new String[]{"~TIME~ give all ","~CAN~1~"," cards ","~NUM~2~"," Power until played, and all ","~CAN~3~"," cards ","~NUM~4~"," Power (wherever they are) until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~3~]'}");

            // (Tlaloc) When drawn, if your deck contains Rain, give that card +22 Power until played.nWhen played, if your deck contains Rain, give your Oceans and Seas cards, wherever they are, +8 Power permanently.
            this.addPattern(new String[]{"When drawn, if your deck contains Rain, give that card +22 Power until played.n" +
                            "When played, if your deck contains Rain, give your Oceans and Seas cards, wherever they are, +8 Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Rain'}," +
                            "'Effect':{'Type':'POWER','Value':'22'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Rain','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'COLLECTION','CompareTo':'Oceans and Seas'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Rain','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[Rain,Oceans and Seas]'}");

            // (Huehuecoyotl) When played, give your Coyote card (wherever it is) +25 Power until played. When played, if it is Round 5, give Coyote +70 Power this turn.
            this.addPattern(new String[]{"When played, give your Coyote card (wherever it is) ","~NUM~1~"," Power until played. When played, if it is Round 5, give Coyote ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Coyote'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Coyote'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'AFTER_ROUND','Value':'4'}]" +
                            "}]," +
                            "'Combos':'[Coyote]'}");

            // (Xolotl) When returned to your deck, give a random card in your hand +28 Power until played. If your deck contains Axolotl, repeat that.
            this.addPattern(new String[]{"When returned to your deck, give a random card in your hand ","~NUM~1~"," Power until played. If your deck contains Axolotl, repeat that."},
                    "{'Effects':[{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented - Currently not the same random card
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented - Currently not the same random card
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Axolotl'}]" +
                            "}]," +
                            "'Combos':'[Axolotl]'}");

            // (Quetzalcoatl) When drawn, if you have played Huītzilōpōchtli this game, give Tetzcatlipoca +100 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~"," this game, give ","~CAN~2~"," ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Mictlantecuhtli) When returned to your deck, if you lost this turn, give a random card remaining in your hand +20 Power, and give another +30 Power for 3 turns.
            this.addPattern(new String[]{"When returned to your deck, if you lost this turn, give a random card remaining in your hand ","~NUM~1~"," Power, and give another ","~NUM~2~"," Power for ","~NUM~3~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented - Currently not the same random card
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented - Currently not the same random card
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Bionic Contact Lens) On return, if you have played The Eye this game, gain +5 power per turn permanently.
            this.addPattern(new String[]{"On return, if you have played ","~CAN~1~"," this game, gain ","~NUM~2~"," power per turn permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Smartdust) When drawn, two of your Opponent's cards have -10 Power this turn.
            this.addPattern(new String[]{"~TIME~ two of your Opponent's cards have ","~NUM~1~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented - Currently not two separate cards
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Laser Sword) When played, for every Forces of the Universe card played this game by either player, give this card +14 Power this turn.
            this.addPattern(new String[]{"~TIME~ for every ","~CAN~1~"," card played this game by either player, give this card ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~2~','CountEach':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','UpTo':'1000','PlayHistory':'TRUE'}}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Cryogenic Freezing) When drawn, lock this card for the rest of this round. nWhile in your hand at the start of each turn, give this card -30 Power until played.
            this.addPattern(new String[]{"~TIME~ lock this card for the rest of this round. nWhile in your hand at the start of each turn, give this card ","~NUM~1~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'LOCK'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Cloaking Device) When played, give 3 random cards in your hand +35 Power this turn. Reveal after scoring.
            this.addPattern(new String[]{"~TIME~ give ","~NUM~1~"," random cards in your hand ","~NUM~2~"," Power this turn. Reveal after scoring."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Powered Exoskeleton) When drawn, give your Science cards +10 Power for 3 turns. Repeat if your deck contains Skeleton.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power for ","~NUM~3~"," turns. Repeat if your deck contains ","~CAN~4~","."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~4~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~4~]'}");

            // (Cloning) When played, give all cards with a Base Power of 22 or less +22 Power until played.
            this.addPattern(new String[]{"~TIME~ give all cards with a Base Power of ","~NUM~1~"," or less ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'BASE_POWER','CompareTo':'<=~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Technological Singularity) When played, if your deck contains Artificial Intelligence, give your Space Technology cards (even if they're in your deck) +22 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your ","~CAN~2~"," cards (even if they're in your deck) ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Doomsday Clock) When played, if it is Turn 3 and you are losing the round, give this card +85 Power this turn.
            this.addPattern(new String[]{"~TIME~ if it is Turn ","~NUM~1~"," and you are losing the round, give this card ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Denny) When drawn, for every multiple of 2 Paleontology cards in both player's decks, give this card +6 Power until played.
            this.addPattern(new String[]{"~TIME~ for every multiple of 2 Paleontology cards in both player's decks, give this card +6 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'3','CountEach':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Paleontology','UpTo':'36','PlayHistory':'FALSE'}}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[Paleontology]'}");

            // (Neanderthal) When drawn, for every Human Evolution card you have played (Max of 4), give your Brilliant Human Body cards (wherever they are) +7 Power until played.nnWhen played, increase the Energy cost of your Brilliant Human Body (wherever they are) cards by 1 until played.
            this.addPattern(new String[]{"When drawn, for every Human Evolution card you have played (Max of 4), give your Brilliant Human Body cards (wherever they are) +7 Power until played.nnWhen played, increase the Energy cost of your Brilliant Human Body (wherever they are) cards by 1 until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Brilliant Human Body'}," +
                            "'Effect':{'Type':'ENERGY','Value':'+1'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Brilliant Human Body'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'7','CountEach':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Human Evolution','UpTo':'4','PlayHistory':'TRUE'}}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[Human Evolution,Brilliant Human Body]'}");

            // (Australopithecus sediba) If played opposite a Paleontology card, give this card +45 Power this turn.
            this.addPattern(new String[]{"If played opposite a Paleontology card, give this card +45 Power this turn."},
                    "NULL"); // TODO: Opposite cards

            // (The Blarney Stone) When played, give a random card in your Opponent's hand -14 power for the rest of the game.
            this.addPattern(new String[]{"~TIME~ give a random card in your Opponent's hand ","~NUM~1~"," power for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Catherine Parr) When returned to your deck, give the other Wives of Henry VIII (even if they're in your deck) +20 Power and reduce their Energy costs by 1 until played.
            this.addPattern(new String[]{"~TIME~ give the other Wives of Henry VIII (even if they're in your deck) +20 Power and reduce their Energy costs by 1 until played."},
                    "NULL"); // TODO: How should subsets of collections be handled? Merry men was very verbose and some replacement thing might be nice

            // (Peking Man) When drawn after Round 2, give a random card in your hand +28 Power for the rest of the game.nWhen returned to your deck, give a random card left in your Opponent's hand -28 Power for the rest of the game.
            this.addPattern(new String[]{"When drawn after Round 2, give a random card in your hand +28 Power for the rest of the game.nWhen returned to your deck, give a random card left in your Opponent's hand -28 Power for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented
                            "'Effect':{'Type':'POWER','Value':'28'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'AFTER_ROUND','Value':'2'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented
                            "'Effect':{'Type':'POWER','Value':'-28'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Friday the 13th) When returned to your deck, if you lost the turn, your Opponent has -36 Power this turn.
            this.addPattern(new String[]{"~TIME~ if you lost the turn, your Opponent has ","~NUM~1~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," + // FIXME: Is this correct or off by one?
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (April Fools) When drawn, if April Fools has been played before, give yourself -2 Energy/Turn for 4 turns.nWhen returned to your deck, give this card -80 Power permanently.
            this.addPattern(new String[]{"~TIME~ if April Fools has been played before, give yourself -2 Energy/Turn for 4 turns.nWhen returned to your deck, give this card -80 Power permanently."},
                    "NULL"); // TODO: Irrelevant card, but difficult & individual pattern -> LATER

            // (Fortune Cookie) At the start of each turn, give a random card in either player's hand +10 Power this turn.
            this.addPattern(new String[]{"At the start of each turn, give a random card in either player's hand ","~NUM~1~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Witching Hour) When played on Rounds 3 or 4, and if your deck contains REM Sleep, give your Horrible Halloween cards (even if they're in your deck) +14 Power until played.
            this.addPattern(new String[]{"When played on Rounds 3 or 4, and if your deck contains REM Sleep, give your Horrible Halloween cards (even if they're in your deck) +14 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Horrible Halloween'}," +
                            "'Effect':{'Type':'POWER','Value':'14'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'AFTER_ROUND','Value':'2'}," +
                                "{'Type':'BEFORE_ROUND','Value':'5'}," +
                                "{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'REM Sleep','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[REM Sleep,Horrible Halloween]'}");

            // (Basan) When played next to Chicken, your Opponent's cards in hand Burn(20) until played.
            this.addPattern(new String[]{"When played next to ","~CAN~1~",", your Opponent's cards in hand Burn(","~NUM~2~",") until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':''," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'BURN','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Who':'SELF','Where':'CARDS_PLAYED','What':'~CAN~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Kintaro) When played on the first turn of the round give this card +70 Power this turn. If you have played Shuten-Doji, reduce this card's energy cost by 3 permanently.
            this.addPattern(new String[]{"When played on the first turn of the round give this card +70 Power this turn. If you have played Shuten-Doji, reduce this card's energy cost by 3 permanently."},
                    "NULL"); // TODO: Irrelevant card, but difficult & individual pattern -> LATER

            // (Urashima Taro) When returned to your deck, give your Ocean Reptiles +25 Power this round.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," ","~NUM~2~"," Power this round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Burkhan Khaldun) When played, give a random Opponent's card -100 Power this turn.
            this.addPattern(new String[]{"~TIME~ give a random Opponent's card ","~NUM~1~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," + // FIXME: Missing ",'What':'RANDOM'" because not implemented
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Ragdoll) When played, give your Dogs cards +14 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Lucrezia Borgia and Giovanni Sforza) When returned to your deck, give all cards (even if they're in either player's deck) -30 Power next turn.
            this.addPattern(new String[]{"~TIME~ give all cards (even if they're in either player's deck) ","~NUM~1~"," Power next turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," + // FIXME: Is this correct or off by one?
                            "}]," +
                            "'Combos':'[]'}");

            // (Andrew Jackson and Rachel Donelson) When returned to your deck, give your Opponent -20 Power/Turn next turn.
            this.addPattern(new String[]{"~TIME~ give your Opponent ","~NUM~1~"," Power/Turn next turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," + // FIXME: Is this correct or off by one?
                            "}]," +
                            "'Combos':'[]'}");

            // (Rocket Lab Electron) When played on the first turn of a round, give this card +18 power this turn.
            this.addPattern(new String[]{"When played on the first turn of a round, give this card ","~NUM~1~"," power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Nopperabo) When played on the first turn of a round, give the card opposite this -20 Power permanently.
            this.addPattern(new String[]{"When played on the first turn of a round, give the card opposite this ","~NUM~1~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," + // TODO: Opposite Target
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Large Hadron Collider) When played on the first turn of a Round, this card has +128 Power.
            this.addPattern(new String[]{"When played on the first turn of a Round, this card has ","~NUM~1~"," Power."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Time Crystals) When played on the first turn of a Round, gain +35 Power/Turn for the rest of the Round.
            this.addPattern(new String[]{"When played on the first turn of a Round, gain ","~NUM~1~"," Power/Turn for the rest of the Round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~1~'}," +
                            "'Duration':'END_ROUND'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Phanes) When played, if it is the first turn of a round, give this card +26 Power this turn. If it is Round 1, give this card an extra +26 Power this turn.
            this.addPattern(new String[]{"~TIME~ if it is the first turn of a round, give this card ","~NUM~1~"," Power this turn. If it is Round 1, give this card an extra ","~NUM~3~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'BEFORE_ROUND','Value':'2'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Tiger"s Eye) When played, give your Cool Cats cards (even if they're in your deck) +13 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards (even if they're in your deck) ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Genetic Inheritance) When played, your Pioneers of Science cards have +14 Power this turn.
            this.addPattern(new String[]{"~TIME~ your ","~CAN~1~"," cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

        } catch (Exception e) {
            e.printStackTrace();
            // ","~CAN~~","
            // ","~NUM~~","
            // ","~N_C~~","
        }
    }
}
