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

                            } else { // COLLECTION, ALBUM or CARD_NAME = CAN
                                if (Album.fromString(toReplace) != null) {
                                    returnString = returnString.replaceAll("~CAN~", "ALBUM");

                                } else if (Collection.fromString(toReplace) != null) {
                                    returnString = returnString.replaceAll("~CAN~", "COLLECTION");

                                } else if (cardNames.contains(toReplace.toLowerCase())) {
                                    returnString = returnString.replaceAll("~CAN~", "NAME");

                                } else {
                                    throw new Exception("Could not find Collection, Album or Card '"+toReplace+"' to replace!");
                                }

                                // Manually telling it what it is
                                returnString = returnString.replaceAll("~C~", "COLLECTION");
                                returnString = returnString.replaceAll("~A~", "ALBUM");
                                returnString = returnString.replaceAll("~N~", "NAME");
                            }
                            returnString = returnString.replaceAll("~" + replace[2] + "~", toReplace);
                        }
                    }

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

            this.addPattern(new String[]{"~TIME~ Lock a random card in your opponent's hand for this turn. If you are losing the round, also give it ","~NUM~1~"," Power until it is played."},
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

            this.addPattern(new String[]{"When this card returns to your deck, steal ","~NUM~1~"," Energy from your opponent."},
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
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~3~','CountEach':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~'}}," +
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
            this.addPattern(new String[]{"~TIME~ for each Curious Cuisine card in your deck, (up to a maximum of four) give the Merry Men, even if they're in your deck, +8 Power until played."},
                    "NULL"); // FIXME: What are Merry Men

            // (Battle of Pelusium (525)) When played, if your deck contains Bastet, give your remaining cards in hand +15 Power permanently.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your remaining cards in hand ","~NUM~~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Great Emu War) When played, for every Birds card in your deck, give your opponent's cards -3 Power this turn.
            this.addPattern(new String[]{"~TIME~ for every ","~CAN~1~"," card in your deck, give your opponent's cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER_FOR_EACH','Value':'~2~','CountEach':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~'}}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Battle of Leipzig (1813)) If played after turn 6 give your opponents cards with 4 or less base energy wherever they are -18 power for 3 turns
            this.addPattern(new String[]{"If played after turn ","~NUM~1~"," give your opponents cards with ","~NUM~2~"," or less base energy wherever they are ","~NUM~3~"," power for ","~NUM~4~"," turns"},
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
                            "'Duration':''," +
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

        } catch (Exception e) {
            e.printStackTrace();
            // ","~CAN~~","
            // ","~NUM~~","
        }
    }
}
