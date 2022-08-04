package Setup;

import Controlling.Main;
import Enums.Album;
import Enums.Collection;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;

@Log4j2
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
        // this.replaceSets();
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
                            choppedNatEffString = choppedNatEffString.substring(endBeforeReplacement + toReplace.length());

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
                                    Main.numErr++;
                                    throw new Exception("Could not find Collection, Album or Card '"+toReplace+"' to replace! NUMBER OF THIS KIND OF EXCEPTION: " + Main.numErr); // TODO: Fix cases
                                }
                            }
                            returnString = returnString.replaceAll("~" + replace[2] + "~", toReplace);
                        }
                    }

                    // Make sure it has read the entire natural effect string
                    if (choppedNatEffString.length() != patternKey[patternKey.length-1].length()) {
                        // TODO: Suggest pattern based on found incomplete pattern
                        log.error("PATTERN NOT COMPLETE for '" + cardname + "': " + Arrays.toString(pattern.natLangKey));
                        foundPattern = false;
                        break;
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

    private void replaceSets() {
        Map<String,String[]> replaceSets = new HashMap<>();
        replaceSets.put("",new String[]{});

        for (Pattern p : this.patterns) {
            String value = p.getJsonOutput();

            if (value.contains("~SET_START~")) {
                int startSetIndex = value.indexOf("~SET_START~") + 11;
                int nameSetIndex = value.indexOf("~",startSetIndex);
                int endSetIndex = value.indexOf("~SET_END~");

                String setName = value.substring(startSetIndex,nameSetIndex);
                String setCopyPaste = value.substring(nameSetIndex+1,endSetIndex);

                String newValue = value.replace("~SET_START~"+setName+"~"+setCopyPaste+"~SET_END~","~SET_CP~");

                // Loop through set and replace accordingly
                // TODO... if I even need the set feature

                // Remove last comma and placeholder
                newValue.replace(",~SET_CP~","");

                System.out.println(setName);
            }
        }
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

            // (Toyger) When played with Bengal Tiger or Sumatran Tiger, give that card +60 Power.
            this.addPattern(new String[]{"When played with ","~CAN~1~"," or ","~CAN~3~",", give that card +","~NUM~2~"," Power."},
                    "{'Effects': [{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~N~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Who':'SELF','Where':'CARDS_PLAYED','What':'~N~','CompareTo':'~1~'}]" +
                            "},{" +
                            "'TriggerTime': 'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~N~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Who':'SELF','Where':'CARDS_PLAYED','What':'~N~','CompareTo':'~3~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~",", give ","~CAN~2~"," and ","~CAN~3~"," ","~NUM~4~"," Power until played."},
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
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'LOCK'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
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

            // (The Sphinx) When played, all Ancient Egypt and Egyptian Mythology cards have +15 Power this turn.
            this.addPattern(new String[]{"~TIME~ all ","~CAN~1~"," and ","~CAN~3~"," cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Beluga Whale) When played, all Oceans & Seas cards have +10 Power this turn.
            this.addPattern(new String[]{"~TIME~ all Oceans & Seas cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Oceans & Seas'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[Oceans & Seas]'}");

            this.addPattern(new String[]{"~TIME~ all ","~CAN~1~",", ","~CAN~4~"," and ","~CAN~3~"," cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects': [{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime': '~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~4~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~,~3~,~4~]'}");

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

            // When drawn, if you have lost at least 1 round, give your Angela Maxwell's Walking the World cards +15 Power this turn.
            this.addPattern(new String[]{"~TIME~ if you have lost at least ","~NUM~1~"," round, give your ","~CAN~2~"," cards ","~NUM~3~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUNDS_LOST','Value':'>~1~'}]" +
                            "}]," +
                            "'Combos':'[~2~]'}");

            // When played, if you have lost two or more rounds, give your Angela Maxwells Walking the World cards +14 Power permanently.
            this.addPattern(new String[]{"~TIME~ if you have lost two or more rounds, give your ","~CAN~1~"," cards ","~NUM~2~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'ROUNDS_LOST','Value':'>~2~'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // While in your hand, at the start of each turn, if you are losing the round, give your Angela Maxwells Walking the World cards +7 Power for 4 turns.
            this.addPattern(new String[]{"While in your hand, at the start of each turn, if you are losing the round, give your ","~CAN~1~"," cards ","~NUM~2~"," Power for ","~NUM~3~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When returned to your deck, if you lost the turn, reduce the Energy cost of your Angela Maxwells Walking the World cards by 2 for 3 turns.
            this.addPattern(new String[]{"~TIME~ if you lost the turn, reduce the Energy cost of your ","~CAN~1~"," cards by ","~NUM~2~"," for ","~NUM~3~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When played, if you are losing the round, give your Angela Maxwells Walking the World cards +25 Power for 3 turns.
            this.addPattern(new String[]{"~TIME~ if you are losing the round, give your ","~CAN~1~"," cards ","~NUM~2~"," Power for ","~NUM~3~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When returned to your deck, if you lost the turn, reduce the Energy cost of your Angela Maxwells Walking the World cards by 1 until played.
            this.addPattern(new String[]{"~TIME~ if you lost the turn, reduce the Energy cost of your ","~CAN~1~"," cards by ","~NUM~2~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // When drawn, if you are losing the round, give your Angela Maxwells Walking the World cards +15 Power this turn.
            this.addPattern(new String[]{"~TIME~ if you are losing the round, give your ","~CAN~1~"," cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Garnet) When played, if you are losing the round, give your adjacent cards +14 Power this turn. Repeat this if it is the last turn of a round.
            this.addPattern(new String[]{"~TIME~ if you are losing the round, give your adjacent cards ","~NUM~2~"," Power this turn. Repeat this if it is the last turn of a round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_PLAYED','What':'RANDOM','Value':'2'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_PLAYED','What':'RANDOM','Value':'2'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}" +
                            ",{'Type':'TURN_IN_ROUND','Value':'3'}]" + // FIXME: Does not work during tie breakers
                            "}]," +
                            "'Combos':'[]'}");

            // (The Flying Dutchman) When played, if you are losing the round, give your cards +48 Power this turn. If you are winning, give your Plundering Pirates cards +36 Power this turn & next
            this.addPattern(new String[]{"When played, if you are losing the round, give your cards +48 Power this turn. If you are winning, give your Plundering Pirates cards +36 Power this turn & next"},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'48'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Loss'}]" +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Plundering Pirates'}," +
                            "'Effect':{'Type':'POWER','Value':'36'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "'Conditions':[{'Type':'ROUND_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[Plundering Pirates]'}");

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

            // (Doge) When played, if your deck contains Shiba Inu, give your Dogs cards +19 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your ","~CAN~2~"," cards ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (The World Turtle) When played, if your deck contains exactly 4 cards from Arts & Culture, give your Arts & Culture cards +17 Power until played. Repeat for each album.
            this.addPattern(new String[]{"~TIME~ if your deck contains exactly ","~NUM~1~"," cards from Arts & Culture, give your Arts & Culture cards ","~NUM~2~"," Power until played. Repeat for each album."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Arts & Culture'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Arts & Culture','Value':'~4~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Oceans & Seas'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Oceans & Seas','Value':'~4~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Space'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Space','Value':'~4~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Life on Land'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Life on Land','Value':'~4~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'History'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'History','Value':'~4~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Paleontology'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Paleontology','Value':'~4~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Science'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Science','Value':'~4~'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Singapura) When returned to your deck, if your deck contains at least 1 Musically Minded card, give your Cute Cats cards +11 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains at least 1 ","~CAN~1~"," card, give your ","~CAN~2~"," cards ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','Value':'>=1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Oriental Shorthair) When drawn, if your deck contains 6 cards from either Feudal Japan or Chinese Folklore, give your cards in hand +15 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~NUM~4~"," cards from either ","~CAN~1~"," or ","~CAN~2~",", give your cards in hand ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','Value':'6'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~','Value':'6'}]" + // FIXME: either or, instead of both
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Alexander Hamilton) When drawn, if your deck contains at least 1 Stage & Screen or Musically Minded card, give your History cards +17 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains at least 1 ","~CAN~1~"," or ","~CAN~2~"," card, give your ","~CAN~3~"," cards ","~NUM~4~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','Value':'>=1'}]" + // FIXME: Or ~CAN~2~
                            "}]," +
                            "'Combos':'[~1~,~2~,~3~]'}");

            // (Nyau) When drawn, if your deck contains a card with Ghost in the name, give your Life on Land cards +15 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains a card with ","~CAN~1~"," in the name, give your ","~CAN~2~"," cards ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N_C~','CompareTo':'~1~','Value':'>=1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Vlad the Impaler) When played, if your deck contains 4 or more Horrible Halloween cards, if the card opposite this has a Base Power of 25 or more, give it -80 Power this turn. When returned to your deck, give your Horrible Halloween cards +13 Power until played.
            this.addPattern(new String[]{"When played, if your deck contains 4 or more Horrible Halloween cards, if the card opposite this has a Base Power of 25 or more, give it -80 Power this turn. When returned to your deck, give your Horrible Halloween cards +13 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_PLAYED','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'-80'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Horrible Halloween','Value':'>=4'}]" + // FIXME: Condition opposite base power
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Horrible Halloween'}," +
                            "'Effect':{'Type':'POWER','Value':'13'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[Horrible Halloween]'}");

            // (Agapornis) When drawn, if your deck contains 3 or more Arts & Culture cards, give your Life on Land cards +20 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~NUM~4~"," or more ","~CAN~1~"," cards, give your ","~CAN~2~"," cards ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~','Value':'>=~4~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Java Man) When drawn, if your deck contains 2 or more Primates cards and 2 or more Brilliant Human Body cards, give your Science and Life on Land cards +8 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~NUM~4~"," or more ","~CAN~1~"," cards and ","~NUM~6~"," or more ","~CAN~5~"," cards, give your ","~CAN~2~"," and ","~CAN~7~"," cards ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','Value':'>=~4~'}," +
                                          "{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~5~','Value':'>=~6~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~7~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~','Value':'>=~4~'}," +
                                          "{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~5~','Value':'>=~6~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~,~5~,~7~]'}");

            // (Pioneer 10) When played, if your deck contains 2 or more Solar System cards, give your Space Technology cards +15 Power until played. Repeat if you have played Jupiter this game.
            this.addPattern(new String[]{"When played, if your deck contains 2 or more Solar System cards, give your Space Technology cards +15 Power until played. Repeat if you have played Jupiter this game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Space Technology'}," +
                            "'Effect':{'Type':'POWER','Value':'15'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Solar System','Value':'>=2'}]" +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Space Technology'}," +
                            "'Effect':{'Type':'POWER','Value':'15'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Solar System','Value':'>=2'}," +
                                          "{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Jupiter'}]" +
                            "}]," +
                            "'Combos':'[Space Technology,Solar System,Jupiter]'}");

                    // (Golden Ratio) When played, if your deck contains 11 Science cards, give your Life on Land cards +19 Power until played.
            this.addPattern(new String[]{"When played, if your deck contains 11 Science cards, give your Life on Land cards +19 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Life on Land'}," +
                            "'Effect':{'Type':'POWER','Value':'19'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Science','Value':'11'}]" +
                            "}]," +
                            "'Combos':'[Science,Life on Land]'}");

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

            // (Humorism) When drawn, give a random card in your hand +50 Power this turn. When played, give this card +20 Power permanently. When returned to your deck, reduce the Energy cost of your Opponent's remaining cards by 2 for 2 turns. While in your hand, at the start of each turn, give your cards +5 Power this turn.
            this.addPattern(new String[]{"When drawn, give a random card in your hand +50 Power this turn. " +
                            "When played, give this card +20 Power permanently. " +
                            "When returned to your deck, reduce the Energy cost of your Opponent's remaining cards by 2 for 2 turns. " +
                            "While in your hand, at the start of each turn, give your cards +5 Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
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

            // (Strawberry Moon) When played, your Curious Cuisine cards gain +20 Power this turn & next.
            this.addPattern(new String[]{"~TIME~ your ","~CAN~1~"," cards gain ","~NUM~2~"," Power this turn & next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Miniature Donkey) When played, give your Little Critters cards +14 Power this turn & next.
            this.addPattern(new String[]{"~TIME~ give your Little Critters cards ","~NUM~1~"," Power this turn & next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Little Critters'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[Little Critters]'}");

            // (Irritator) When played, give your Raging Rivers and Ocean Reptiles cards +18 Power this turn & next.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power this turn & next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (O.K. Corral) When played, give your Legends of the Old West cards +15 Power this turn and give two random cards in your Opponent's hand -9 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power this turn and give two random cards in your Opponent's hand ","~NUM~3~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_PLAYED','What':'RANDOM','Value':'2'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Hecate) When played, give your Dogs, Plant Life and Venomous Creatures cards +8 power this turn & next. If you have played Zombies this game, give them an additional +8 Power this turn & next.
            this.addPattern(new String[]{"When played, give your Dogs, Plant Life and Venomous Creatures cards +8 power this turn & next. If you have played Zombies this game, give them an additional +8 Power this turn & next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Dogs'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Plant Life'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Venomous Creatures'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Zombies'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Zombies'}]" +
                            "}]," +
                            "'Combos':'[Dogs,Plant Life,Venomous Creatures,Zombies]'}");

            // (Ground Cuscus) When played, give your Opponent's Tremendous Trees and Plant Life cards -60 Power this turn & next.
            this.addPattern(new String[]{"~TIME~ give your Opponent's ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power this turn & next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Luhman 16) When drawn, give your Space cards in hand +14 Power this turn & next.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards in hand ","~NUM~2~"," Power this turn & next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Waimanu) When played, give your Birds and Sea Birds cards in hand +12 Power this turn & next. When returned to your deck, reduce their Energy cost by 1 for the rest of the game.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~2~"," cards in hand ","~NUM~3~"," Power this turn & next. When returned to your deck, reduce their Energy cost by ","~NUM~4~"," for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~4~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~4~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Flower Moon) When played, give your Plant Life cards +20 Power this turn & next.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power this turn & next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Dante Alighieri) When played, give your Legendary cards +17 Power this turn & next.
            this.addPattern(new String[]{"~TIME~ give your Legendary cards ","~NUM~2~"," Power this turn & next."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Lgnd'}," +
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

            // (Robin Hood) When played, give your Opponent's cards with 80 or more Base Power -20 Power this turn and give the Merry Men, even if they're in your deck, +20 Power permanently.
            this.addPattern(new String[]{"When played, give your Opponent's cards with 80 or more Base Power -20 Power this turn and give the Merry Men, even if they're in your deck, +20 Power permanently."},
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

            // (Piltdown Man) When played, if your deck contains The Brain, give your Primates and Human Evolution cards +12 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your ","~CAN~2~"," and ","~CAN~3~"," cards ","~NUM~4~"," Power until played."},
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

            // (Kuiper Belt) When returned to your deck, if your deck contains Makemake, give your Space cards +8 Power until played. Repeat for Eris and Pluto; Dwarf Planet.
            this.addPattern(new String[]{"When returned to your deck, if your deck contains Makemake, give your Space cards +8 Power until played. Repeat for Eris and Pluto; Dwarf Planet."},
                    "{'Effects':[{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Space'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Makemake','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Space'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Eris','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Space'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Pluto; Dwarf Planet','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[Makemake,Space,Eris,Pluto; Dwarf Planet]'}");

            // () When drawn, if your deck contains Giant Panda, give your Crustaceans, Cephalopods, Feisty Fish and Fabulous Fish cards +13 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your ","~CAN~2~",", ","~CAN~5~"," and ","~CAN~3~"," cards ","~NUM~4~"," Power until played."},
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
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~5~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~,~3~,~5~]'}");

            // (Commerson"s Dolphin) When drawn, if your deck contains Giant Panda, give your Crustaceans, Cephalopods, Feisty Fish and Fabulous Fish cards +13 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~CAN~1~",", give your ","~CAN~2~",", ","~CAN~5~",", ","~CAN~6~"," and ","~CAN~3~"," cards ","~NUM~4~"," Power until played."},
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
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~5~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~6~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~,~3~,~5~,~6~]'}");

            // (Delta IV Heavy) When played, if your deck contains 2 or more cards from Life on Land and 2 or more cards from Oceans & Seas, give your Space cards, wherever they are, +16 Power until played.
            this.addPattern(new String[]{"~TIME~ if your deck contains ","~NUM~4~"," or more cards from ","~CAN~1~"," and ","~NUM~5~"," or more cards from ","~CAN~2~",", give your ","~CAN~3~"," cards, wherever they are, ","~NUM~6~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~6~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~1~','Value':'>=~4~'}," +
                                          "{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~2~','Value':'>=~5~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~,~3~]'}");

            // (Demon) When played, if your deck contains Hel, give your Norse Mythology cards +30 Power until played. Repeat for Hades and Greek Mythology, and Osiris and Egyptian Mythology.
            this.addPattern(new String[]{"When played, if your deck contains Hel, give your Norse Mythology cards +30 Power until played. Repeat for Hades and Greek Mythology, and Osiris and Egyptian Mythology."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Norse Mythology'}," +
                            "'Effect':{'Type':'POWER','Value':'30'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Hel','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Greek Mythology'}," +
                            "'Effect':{'Type':'POWER','Value':'30'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Hades','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Egyptian Mythology'}," +
                            "'Effect':{'Type':'POWER','Value':'30'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Osiris','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[Norse Mythology,Hel,Greek Mythology,Hades,Osiris,Egyptian Mythology]'}");

            // (Alexander Hamilton) When drawn, if your deck contains at least 1 Stage and Screen or Musically Minded card, give your History cards +17 Power until played.
            this.addPattern(new String[]{"When drawn, if your deck contains at least 1 Stage and Screen or Musically Minded card, give your History cards +17 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~History~'}," +
                            "'Effect':{'Type':'POWER','Value':'10'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Stage and Screen','Value':'>=1'}]" + // FIXME: Condition is either and not or
                            "},{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~History~'}," +
                            "'Effect':{'Type':'POWER','Value':'10'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Musically Minded','Value':'>=1'}]" +
                            "}]," +
                            "'Combos':'[Stage and Screen,Musically Minded]'}");

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
                            "{'Who':'SELF','Where':'CARDS_IN_DECK','What':'THIS','UpTo':'18','PlayHistory':'TRUE'}}," +
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

            // (D.B. Cooper) When drawn, lock this card in your hand for the rest of the round. When played, for every Awesome Aviation card played this game by either player (up to a maximum of 18), give this card +10 Power this turn. When returned to your deck, give your Money, Money, Money cards, wherever they are, +20 Power until played.
            this.addPattern(new String[]{"When drawn, lock this card in your hand for the rest of the round. " +
                            "When played, for every ","~CAN~1~"," card played this game by either player (up to a maximum of 18), give this card +10 Power this turn. " +
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

            // (Mary Toft"s Rabbit Birth) When returned to your deck, reduce the Energy cost of your Marvellous Medicine cards by 1 until played.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your ","~CAN~1~"," cards by ","~NUM~2~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Cornucopia) When drawn, reduce the Energy cost of your Curious Cuisine and Plant Life cards by 1 until played.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your ","~CAN~1~"," and ","~CAN~3~"," cards by ","~NUM~2~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Atlas V) When returned to your deck, reduce the Energy cost of your Centaur, Centaurus and Alpha Centauri cards by 2 until played.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your ","~CAN~1~",", ","~CAN~4~"," and ","~CAN~3~"," cards by ","~NUM~2~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~4~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~3~,~4~]'}");

            // (Magna Carta) When drawn, reduce the Energy cost of your Epic and Rare cards by 2 until played.
            //               When played with Declaration of Independence or Universal Declaration of Human Rights , give them +20 Power this turn.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your Epic and Rare cards by ","~NUM~4~"," until played. " +
                            "When played with ","~CAN~1~"," or ","~CAN~2~"," , give them ","~NUM~3~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Rare'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Epic'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Who':'SELF','Where':'CARDS_PLAYED','What':'~CAN~','CompareTo':'~1~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Who':'SELF','Where':'CARDS_PLAYED','What':'~CAN~','CompareTo':'~2~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Tammar Wallaby) When drawn, reduce the Energy cost of your Marsupials cards by 1, and give them +7 Power, until played.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your ","~CAN~1~"," cards by ","~NUM~2~",", and give them ","~NUM~3~"," Power, until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Luxor Temple) When returned to your deck, reduce the Energy cost of your Common History cards by 1 until played.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your Common ","~CAN~1~"," cards by ","~NUM~2~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: RARITY
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Griffith Observatory) When drawn, reduce the Energy cost of your Common and Rare Space cards by 2 until played.
            this.addPattern(new String[]{"~TIME~ reduce the Energy cost of your Common and Rare ","~CAN~1~"," cards by ","~NUM~2~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: RARITY
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Drop Bear) When played, if you have played Koala, your Carnivores cards gain +18 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~",", your ","~CAN~2~"," cards gain ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Raccoon) When played, if you have played a Radical Rockets card this game, give your Life on Land cards in hand +34 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played a ","~CAN~1~"," card this game, give your ","~CAN~2~"," cards in hand ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~A~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Blue Crayfish) When played, if you have played Sapphire this game, give your Crustaceans cards +26 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~"," this game, give your ","~CAN~2~"," cards ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~C~','CompareTo':'~2~'}," +
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
            this.addPattern(new String[]{"When played in either the left or right slot, give your ","~CAN~1~"," cards ","~NUM~2~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Mycoplasma) When played, reduce the power of your Opponent's card opposite by 50, and reduce its energy cost by 2, for the rest of the game.
            this.addPattern(new String[]{"~TIME~ reduce the power of your Opponent's card opposite by ","~NUM~1~",", and reduce its energy cost by ","~NUM~2~",", for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_PLAYED','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'-~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_PLAYED','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

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

            // (Great Dying) When returned to your deck, for every Paleontology card you have played this game, give your Paleontology cards -10 Power permanently, and give your Life on Land and Oceans & Seas cards +6 Power permanently.
            this.addPattern(new String[]{"~TIME~ for every Paleontology card you have played this game, give your Paleontology cards -10 Power permanently, and give your Life on Land and Oceans & Seas cards +6 Power permanently."},
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

            // (Lepidodendron) When drawn, give your Common and Rare Reptiles and Tremendous Trees cards +20 Power permanently.
            this.addPattern(new String[]{"~TIME~ give your Common and Rare ","~CAN~1~"," and ","~CAN~2~"," cards ","~NUM~3~"," Power permanently."},
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

            // (Chicxulub Crater) When played, give all Paleontology cards -7 Power until played, and all Space cards +7 Power until played.
            this.addPattern(new String[]{"~TIME~ give all ","~CAN~1~"," cards ","~NUM~2~"," Power until played, and all ","~CAN~3~"," cards ","~NUM~4~"," Power until played."},
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

            // (Tlaloc) When drawn, if your deck contains Rain, give that card +22 Power until played. When played, if your deck contains Rain, give your Oceans & Seas cards, wherever they are, +8 Power permanently.
            this.addPattern(new String[]{"When drawn, if your deck contains Rain, give that card +22 Power until played. " +
                            "When played, if your deck contains Rain, give your Oceans & Seas cards, wherever they are, +8 Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Rain'}," +
                            "'Effect':{'Type':'POWER','Value':'22'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Rain','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'COLLECTION','CompareTo':'Oceans & Seas'}," +
                            "'Effect':{'Type':'POWER','Value':'8'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'NAME','CompareTo':'Rain','Value':'1'}]" +
                            "}]," +
                            "'Combos':'[Rain,Oceans & Seas]'}");

            // (Huehuecoyotl) When played, give your Coyote card +25 Power until played. When played, if it is Round 5, give Coyote +70 Power this turn.
            this.addPattern(new String[]{"When played, give your Coyote card ","~NUM~1~"," Power until played. When played, if it is Round 5, give Coyote ","~NUM~2~"," Power this turn."},
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
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
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

            // (Bunbuku Chagama) When played, if you have played Tanuki this game, give it +75 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~"," this game, give it ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Houndstooth) When drawn, if you have played Houndstooth this game, give this card +56 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~"," this game, give this card ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Warty Crab) When played, if you have played Oliver Cromwell this game, give it and Warthog +32 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~"," this game, give it and ","~CAN~2~"," ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Dr. Jekyll) When drawn, if you have played Mr Hyde this game, give it and this card +35 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played ","~CAN~1~"," this game, give it and this card ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Common Tailorbird) When played, if you have played a Fancy Fashions card this game, give your Birds cards +18 Power until played.
            this.addPattern(new String[]{"~TIME~ if you have played a ","~CAN~1~"," card this game, give your ","~CAN~2~"," cards ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[~1~,~2~]'}");

            // (Mictlantecuhtli) When returned to your deck, if you lost this turn, give a random card remaining in your hand +20 Power, and give another +30 Power for 3 turns.
            this.addPattern(new String[]{"When returned to your deck, if you lost this turn, give a random card remaining in your hand ","~NUM~1~"," Power, and give another ","~NUM~2~"," Power for ","~NUM~3~"," turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~3~'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING','What':'RANDOM','Value':'1'}," +
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
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'2'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Okapi) When played, your Opponent's cards have -10 Power this turn.
            this.addPattern(new String[]{"~TIME~ your Opponent's cards have ","~NUM~1~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," +
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
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'3'}," +
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

            // () When played, give all cards with a Base Power of 22 or less +22 Power until played.
            this.addPattern(new String[]{"~TIME~ give all cards with a Base Power of ","~NUM~1~"," or less ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK','What':'BASE_POWER','CompareTo':'<=~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Cloning) When played, give all cards in hand with a Base Power of 22 or less +22 Power until played.
            this.addPattern(new String[]{"~TIME~ give all cards in hand with a Base Power of ","~NUM~1~"," or less ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_HAND','What':'BASE_POWER','CompareTo':'<=~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

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

            // (Neanderthal) When drawn, for every Human Evolution card you have played (Max of 4), give your Brilliant Human Body cards +7 Power until played. When played, increase the Energy cost of your Brilliant Human Body cards by 1 until played.
            this.addPattern(new String[]{"When drawn, for every Human Evolution card you have played (Max of 4), give your Brilliant Human Body cards +7 Power until played. When played, increase the Energy cost of your Brilliant Human Body cards by 1 until played."},
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
            this.addPattern(new String[]{"If played opposite a ","~CAN~1~"," card, give this card ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'PLAYED_WITH','Who':'OTHER','Where':'CARDS_PLAYED','What':'~CAN~','CompareTo':'~1~'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (The Blarney Stone) When played, give a random card in your Opponent's hand -14 power for the rest of the game.
            this.addPattern(new String[]{"~TIME~ give a random card in your Opponent's hand ","~NUM~1~"," power for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Peking Man) When drawn after Round 2, give a random card in your hand +28 Power for the rest of the game. When returned to your deck, give a random card left in your Opponent's hand -28 Power for the rest of the game.
            this.addPattern(new String[]{"When drawn after Round 2, give a random card in your hand +28 Power for the rest of the game. When returned to your deck, give a random card left in your Opponent's hand -28 Power for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'28'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'AFTER_ROUND','Value':'2'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_REMAINING','What':'RANDOM','Value':'1'}," +
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
                            "'Duration':{'Type':'TIMER','Value':'1'}," + // TODO: It NEVER returns to normal... WHY?!?
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (April Fools) When drawn, if April Fools has been played before, give yourself -2 Energy/Turn for 4 turns. When returned to your deck, give this card -80 Power permanently.
            this.addPattern(new String[]{"When drawn, if April Fools has been played before, give yourself ","~NUM~1~"," Energy/Turn for ","~NUM~2~"," turns. " +
                            "When returned to your deck, give this card ","~NUM~3~"," Power permanently."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY_PER_TURN','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'~2~'}," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'April Fools'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Fortune Cookie) At the start of each turn, give a random card in either player's hand +10 Power this turn.
            this.addPattern(new String[]{"At the start of each turn, give a random card in either player's hand ","~NUM~1~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'START'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Witching Hour) When played on Rounds 3 or 4, and if your deck contains REM Sleep, give your Horrible Halloween cards +14 Power until played.
            this.addPattern(new String[]{"When played on Rounds 3 or 4, and if your deck contains REM Sleep, give your Horrible Halloween cards +14 Power until played."},
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
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'70'}," +
                            "'Duration':'END_TURN'," +
                            "'Conditions':[{'Type':'TURN_IN_ROUND','Value':'1'}]" +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-3'}," +
                            "'Duration':'PERMANENT'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Shuten-Doji'}]" +
                            "}]," +
                            "'Combos':'[Shuten-Doji]'}");

            // (Urashima Taro) When returned to your deck, give your Ocean Reptiles +25 Power this round.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," +","~NUM~2~"," Power this round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Ship of Theseus) When drawn, give your Riding the Waves cards +20 Power this round.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards +","~NUM~2~"," Power this round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Urashima Taro) When returned to your deck, give your Ocean Reptiles +25 Power this round.
            this.addPattern(new String[]{"When returned to your deck, give your Birds and Sea Birds cards and Swans +12 Power this round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Birds'}," +
                            "'Effect':{'Type':'POWER','Value':'+12'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Sea Birds'}," +
                            "'Effect':{'Type':'POWER','Value':'+12'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Swans'}," +
                            "'Effect':{'Type':'POWER','Value':'+12'}," +
                            "'Duration':'END_ROUND'," +
                            "}]," +
                            "'Combos':'[Birds,Sea Birds,Swans]'}");

            // (Olive Python) When played, give your Reptiles and Ocean Reptiles cards +14 Power this round.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards +","~NUM~2~"," Power this round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Red Panda) When returned to your deck, give your Plant Life, Curious Cuisine and Carnivores cards +22 Power this round.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~",", ","~CAN~4~"," and ","~CAN~3~"," cards +","~NUM~2~"," Power this round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~4~'}," +
                            "'Effect':{'Type':'POWER','Value':'+~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "}]," +
                            "'Combos':'[~1~,~3~,~4~]'}");

            // (Burkhan Khaldun) When played, give a random Opponent's card -100 Power this turn.
            this.addPattern(new String[]{"~TIME~ give a random Opponent's card ","~NUM~1~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'RANDOM','Value':'1'}," +
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

            // (Sedlec Ossuary) When played, give your Skeleton card +58 Power this turn. When returned to your deck, increase the Energy cost of your Opponent's Sue Blacks In The Bones cards by 1 until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," card ","~NUM~2~"," Power this turn. When returned to your deck, increase the Energy cost of your Opponent's Sue Blacks In The Bones cards by 1 until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Sue Blacks In The Bones'}," +
                            "'Effect':{'Type':'ENERGY','Value':'+1'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Abyssinian) When played, give your other cards -100 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your other cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Declaration of Independence) When played, give your other cards -50 Power this turn. If you win this turn, give the cards left in your hand +20 power next turn.
            this.addPattern(new String[]{"~TIME~ give your other cards -","~NUM~1~"," Power this turn. If you win this turn, give the cards left in your hand ","~NUM~2~"," power next turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'-~1~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[]'}");

            // (Nezha) When played, give your other cards +10 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your other cards +","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'-~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Banded Mongoose) When played, give your other Life on Land cards +19 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your other ","~CAN~1~"," cards +","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'-~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // () When played, give your Awesome Aviation and Something cards +14 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Dwarf) When played, give your cards +15 Power and your Opponent's cards -15 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your cards ","~NUM~1~"," Power and your Opponent's cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Chinlone) When played, give your Good Sports cards and Birman +15 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards and ","~CAN~3~"," +","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Hamburger) When played, give your other cards +35 Power this turn and give this card -20 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your other cards +","~NUM~1~"," Power this turn and give this card ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'-~1~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Ibn al-Nafis) When played, give your Documented cards and Blood Circulation +20 Power this turn.
            this.addPattern(new String[]{"When played, give your Documented cards and Blood Circulation +20 Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Documented'}," +
                            "'Effect':{'Type':'POWER','Value':'20'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Blood Circulation'}," +
                            "'Effect':{'Type':'POWER','Value':'20'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[Documented,Blood Circulation]'}");

            // (Virtual Reality) When played, give your cards +15 Power this turn. When returned to your deck, reduce the Energy cost of cards left in your hand by 2 next turn.
            this.addPattern(new String[]{"~TIME~ give your cards ","~NUM~1~"," Power this turn. When returned to your deck, reduce the Energy cost of cards left in your hand by ","~NUM~2~"," next turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_REMAINING'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~2~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "}]," +
                            "'Combos':'[]'}");

            // (La Chupaleche) When played, give your Common cards +18 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Common cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Cmmn'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // () When played, give your Rare cards +18 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Rare cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Rare'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // () When played, give your Epic cards +18 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Epic cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Epic'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Sistine Chapel) When played, give your Legendary cards +26 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Legendary cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Lgnd'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Mechanical Clock) When played, give your Opponent's cards -12 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Opponent's cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Monkey Orchid) When played, give your Epic Primates cards +30 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Epic ","~CAN~1~"," cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: Rarity
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Hatshepsut) When played, give your Female Unruly Rulers +30 Power until played and give your Ancient Egypt and Egyptian Mythology cards +18 Power this turn.
            this.addPattern(new String[]{"When played, give your Female Unruly Rulers +30 Power until played and give your Ancient Egypt and Egyptian Mythology cards +18 Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Unruly Rulers'}," + // FIXME: Female only
                            "'Effect':{'Type':'POWER','Value':'30'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~C~','CompareTo':'Ancient Egypt'}," +
                            "'Effect':{'Type':'POWER','Value':'18'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~C~','CompareTo':'Egyptian Mythology'}," +
                            "'Effect':{'Type':'POWER','Value':'18'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[Unruly Rulers,Ancient Egypt,Egyptian Mythology]'}");

            // (Sandwich Theory) When played, give your other cards +30 Power and give this card -30 Power this turn.
            this.addPattern(new String[]{"When played, give your other cards +30 Power and give this card -30 Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_PLAYED','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'30'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Archegosaurus) When played, give your Amphibians, Feisty Fish and Fabulous Fish cards +15 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~",", ","~CAN~3~"," and ","~CAN~4~"," cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~4~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~,~4~,~3~]'}");

            // (The Bermuda Triangle) When played, give your Oceans & Seas cards +18 Power this turn.
            // When returned to your deck, reduce the Energy cost of your Riding the Waves! and Awesome Aviation cards by 2 for the rest of the round.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power this turn. " +
                            "When returned to your deck, reduce the Energy cost of your ","~CAN~3~"," and ","~CAN~4~"," cards by ","~NUM~5~"," for the rest of the round."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~5~'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'~4~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~5~'}," +
                            "'Duration':'END_ROUND'," +
                            "}]," +
                            "'Combos':'[~1~,~4~,~3~]'}");

            // (Pocahontas & John Smith) When played, give your History and Arts & Culture cards +15 Power this turn. If you win this turn, gain +2 Energy/Turn for the rest of the round
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~4~"," cards ","~NUM~2~"," Power this turn. If you win this turn, gain ","~NUM~3~"," Energy/Turn for the rest of the round"},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~4~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'ENERGY_PER_TURN','Value':'~3~'}," +
                            "'Duration':'END_ROUND'," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[~1~,~4~]'}");

            // (Discovery Expedition) When played, give your Dizzying Discoveries and Riding the Waves! cards +10 Power this turn. When returned to your deck, reduce the energy cost of your Exciting Exploration cards by 1 until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power this turn. When returned to your deck, reduce the energy cost of your ","~CAN~4~"," cards by ","~NUM~5~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~4~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~5~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~3~,~4~]'}");

            // (Resplendent Quetzal) When played, give your Plant Life and Tremendous Trees cards +25 Power this turn. If you win the turn, they keep 10 until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power this turn. If you win the turn, they keep ","~NUM~5~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~5~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~5~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Greenhouse Gases) When played, give your Opponent's Our Planet cards -14 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Opponent's ","~CAN~1~"," cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Tinikling) When played, give your Opponent's History and Paleontology cards -19 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Opponent's ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Anne Bonny) When played, give your Opponent's cards -8 Power this turn, and give Calico Jack +35 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Opponent's cards ","~NUM~2~"," Power this turn, and give Calico Jack ","~NUM~3~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~N~','CompareTo':'Calico Jack'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[Calico Jack]'}");

            // (Circe) When played, give your Opponent's Legendary cards -20 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your Opponent's Legendary cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Lgnd'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Snowflakes) When played, give your adjacent cards -70 Power this turn.
            this.addPattern(new String[]{"~TIME~ give your adjacent cards ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_PLAYED','What':'RANDOM','Value':'1'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

// TODO: Why in the world does THIS lead to an infinite loop, but only when it's BOTH or SELF & OTHER in separate effects?!
//
//            // (Lucrezia Borgia & Giovanni Sforza) When returned to your deck, give all cards -30 Power next turn.
//            this.addPattern(new String[]{"~TIME~ give all cards ","~NUM~1~"," Power next turn."},
//                    "{'Effects':[{" +
//                            "'TriggerTime':'~TIME~'," +
//                            "'Target':{'Who':'BOTH','Where':'CARDS_IN_DECK'}," +
//                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
//                            "'Duration':{'Type':'TIMER','Value':'1'}," +
//                            "}]," +
//                            "'Combos':'[]'}");
//
//            // (Lucrezia Borgia & Giovanni Sforza) When returned to your deck, give all cards -30 Power next turn.
//            this.addPattern(new String[]{"~TIME~ give all cards ","~NUM~1~"," Power next turn."},
//                    "{'Effects':[{" +
//                            "'TriggerTime':'~TIME~'," +
//                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK'}," +
//                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
//                            "'Duration':{'Type':'TIMER','Value':'1'}," +
//                            "},{" +
//                            "'TriggerTime':'~TIME~'," +
//                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK'}," +
//                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
//                            "'Duration':{'Type':'TIMER','Value':'1'}," +
//                            "}]," +
//                            "'Combos':'[]'}");

            // (Andrew Jackson & Rachel Donelson) When returned to your deck, give your Opponent -20 Power/Turn next turn.
            this.addPattern(new String[]{"~TIME~ give your Opponent ","~NUM~1~"," Power/Turn next turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~1~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
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
                            "'Target':{'Who':'OTHER','Where':'CARDS_PLAYED','What':'RANDOM','Value':'1'}," +
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

            // (Tiger"s Eye) When played, give your Cool Cats cards +13 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Fairy Circle) When returned to your deck, give your other cards with Fairy in the title +15 Power until played.
            this.addPattern(new String[]{"~TIME~ give your other cards with ","~N_C~2~"," in the title +","~NUM~1~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N_C~','CompareTo':'~2~'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'-~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Lungs) When drawn, give your Five Senses cards -5 Power until played. When returned to your deck, give them +10 Power for the rest of the game.
            this.addPattern(new String[]{"When drawn, give your Five Senses cards ","~NUM~1~"," Power until played. When returned to your deck, give them ","~NUM~2~"," Power for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Taste'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Taste'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Touch'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Touch'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Sight'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Sight'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Smell'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Smell'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'DRAW'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Hearing'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Hearing'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[Taste,Touch,Sight,Smell,Hearing]'}");

            // (Cyclopes) When drawn, give your Opponent's Legendary or Mythic cards -12 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Opponent's Legendary or Mythic cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Mthc'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Lgnd'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Fairy Chimneys) When returned to your deck, give your Mythic cards +20 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Mythic cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Mthc'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Star-nosed Mole) When returned to your deck, give your Opponent's cards -9 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Opponent's cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Kronos) When played, give your cards in hand with a Base Energy cost of 5 or more +25 Power this round. When returned to your deck give your cards with a Base Energy cost of 4 or less -20 Power until played.
            this.addPattern(new String[]{"When played, give your cards in hand with a Base Energy cost of 5 or more +25 Power this round. When returned to your deck give your cards with a Base Energy cost of 4 or less -20 Power until played."},
                    "NULL"); // TODO: New Target based on energy

            // (Handwashing) When played, give your Common Science cards +12 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Common ","~CAN~1~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: Rarity
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Tanzanite) When drawn, give your Rare Hidden Gems cards +14 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Rare ","~CAN~1~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: Rarity
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            this.addPattern(new String[]{"~TIME~ give your Epic ","~CAN~1~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: Rarity
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            this.addPattern(new String[]{"~TIME~ give your Legendary ","~CAN~1~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: Rarity
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Subrahmanyan Chandrasekhar) When drawn, give your Common and Rare Watching the Skies cards +19 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Common and Rare ","~CAN~1~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: Rarity
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Confucius) When drawn, give your 7 Sages of Ancient Greece cards +21 Power until played.
            this.addPattern(new String[]{"~TIME~ give your 7 Sages of Ancient Greece cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Ancient Greece'}," + // FIXME: Seven Sages Set
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[Ancient Greece]'}");

            // (Lyle"s Flying Fox) When played, give your Mammals and Birds cards +8 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Catherine of Aragon) When returned to your deck, give your Turbulent Tudors cards +15 Power until played and give your Documented cards +20 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power until played and give your ","~CAN~3~"," cards ","~NUM~4~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Trebuchet) When played, give your Opponent’s Super Structures and Wonders of Construction cards -20 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Opponent’s ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Osiris Blue) When played, give your Egyptian Mythology cards in hand +2 Energy and +36 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards in hand ","~NUM~2~"," Energy and ","~NUM~3~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Domitian) When drawn, give your Legendary cards -25 Power until played and give your Rare and Common Cards +15 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Legendary cards ","~NUM~1~"," Power until played and give your Rare and Common Cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Lgnd'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Rare'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'RARITY','CompareTo':'Cmmn'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Galileo Satellite System) When played, give your Awesome Aviation and Riding the Waves cards +25 Power until played. If you lose the turn, gain +15 Power/Turn next turn.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power until played. If you lose the turn, gain ","~NUM~4~"," Power/Turn next turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'~4~'}," +
                            "'Duration':{'Type':'TIMER','Value':'1'}," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Loss'}]" +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Moorish Idol) When played, give your Fabulous Fish, Sharks! and Deep Ocean cards +12 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~",", ","~CAN~4~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~4~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~3~,~4~]'}");

            // (Yeti) When played, give your Opponent -5 Power/Turn for the rest of the game, and give your cards in hand with a Base Energy of 7 or higher +20 Power until played.
            this.addPattern(new String[]{"When played, give your Opponent -5 Power/Turn for the rest of the game, and give your cards in hand with a Base Energy of 7 or higher +20 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'OTHER'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'-5'}," +
                            "'Duration':'PERMANENT'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'BASE_ENERGY','CompareTo':'>=7'}," +
                            "'Effect':{'Type':'POWER','Value':'20'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Wolfgang Amadeus Mozart) When played, give your Musically Minded and Instrumental cards +16 Power until played. If your deck contains 6 or more History cards, give them an additional +14 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power until played. If your deck contains 6 or more History cards, give them an additional +14 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'14'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'History','Value':'>=6'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'14'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'History','Value':'>=6'}]" +
                            "}]," +
                            "'Combos':'[~1~,~3~,History]'}");

            // (The Lady of the Lake) When played, give your Oceans & Seas cards +20 Power until the end of the round, and give Excalibur +50 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power until the end of the round, and give ","~CAN~3~"," ","~NUM~4~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_ROUND'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Grant Wood) When played, give your Innovations of War and Grand Designs cards +21 Power until played. If your deck contains 6 or more Arts & Culture cards, do it again.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power until played. If your deck contains 6 or more Arts & Culture cards, do it again."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Arts & Culture','Value':'>=6'}]" +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'DECK_CONTAINS','Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Arts & Culture','Value':'>=6'}]" +
                            "}]," +
                            "'Combos':'[~1~,~3~,Arts & Culture]'}");

            // (Hilma af Klint) When drawn, give your Opponent's Science cards -15 Power until played, and give the Grim Reaper +30 Power until played.
            this.addPattern(new String[]{"~TIME~ give your Opponent's ","~CAN~1~"," cards ","~NUM~2~"," Power until played, and give the Grim Reaper +30 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Grim Reaper'}," +
                            "'Effect':{'Type':'POWER','Value':'30'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[Grim Reaper]'}");

            // (Pizza) When drawn, give your Curious Cuisine cards +12 Power until played, and give Fangtooth Moray +50 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," cards ","~NUM~2~"," Power until played, and give Fangtooth Moray +50 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Fangtooth Moray'}," +
                            "'Effect':{'Type':'POWER','Value':'50'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,Fangtooth Moray]'}");

            // (Cthulhu) When played, give your Oceans & Seas cards +30 Power until played. When returned to your deck, give them -20 Power until played and give your Opponent -10 Power/Turn for 2 turns.
            this.addPattern(new String[]{"When played, give your Oceans & Seas cards +30 Power until played. When returned to your deck, give them -20 Power until played and give your Opponent -10 Power/Turn for 2 turns."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Oceans & Seas'}," +
                            "'Effect':{'Type':'POWER','Value':'30'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~A~','CompareTo':'Oceans & Seas'}," +
                            "'Effect':{'Type':'POWER','Value':'-20'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'OTHER'}," +
                            "'Effect':{'Type':'POWER_PER_TURN','Value':'-10'}," +
                            "'Duration':{'Type':'TIMER','Value':'2'}," +
                            "}]," +
                            "'Combos':'[Oceans & Seas]'}");

            // (Library of Alexandria) When played, give your History and Science cards +38 Power this turn. When returned to your deck, give this card -40 Power until played.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," and ","~CAN~3~"," cards ","~NUM~2~"," Power this turn. When returned to your deck, give this card ","~NUM~4~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'THIS'}," +
                            "'Effect':{'Type':'POWER','Value':'~4~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (William Thomson; 1st Baron Kelvin) When returned to your deck, give your Dizzying Discoveries and Forces of the Universe cards +12 Power until played. If you have played Absolute Zero this game, do it again.
            this.addPattern(new String[]{"When returned to your deck, give your Dizzying Discoveries and Forces of the Universe cards +12 Power until played. If you have played Absolute Zero this game, do it again."},
                    "{'Effects':[{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Dizzying Discoveries'}," +
                            "'Effect':{'Type':'POWER','Value':'12'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Forces of the Universe'}," +
                            "'Effect':{'Type':'POWER','Value':'12'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Dizzying Discoveries'}," +
                            "'Effect':{'Type':'POWER','Value':'12'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Absolute Zero'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Forces of the Universe'}," +
                            "'Effect':{'Type':'POWER','Value':'12'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'PLAYED_BEFORE','Who':'SELF','Where':'CARDS_IN_DECK','What':'~N~','CompareTo':'Absolute Zero'}]" +
                            "}]," +
                            "'Combos':'[Dizzying Discoveries,Forces of the Universe,Absolute Zero]'}");

            // (Butcherbird) When played, give your Bugs and Reptiles cards +10 Power until played. When returned to your deck, give your Horrible Halloween cards +9 Power until played.
            this.addPattern(new String[]{"When played, give your Bugs and Reptiles cards +10 Power until played. When returned to your deck, give your Horrible Halloween cards +9 Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Bugs'}," +
                            "'Effect':{'Type':'POWER','Value':'10'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Reptiles'}," +
                            "'Effect':{'Type':'POWER','Value':'10'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Horrible Halloween'}," +
                            "'Effect':{'Type':'POWER','Value':'9'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[Bugs,Reptiles,Horrible Halloween]'}");

            // (Genetic Inheritance) When played, your Pioneers of Science cards have +14 Power this turn.
            this.addPattern(new String[]{"~TIME~ your ","~CAN~1~"," cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Catherine Howard) When played, if you are losing the round, give your cards +12 Power this turn. When returned to your deck, give your Turbulent Tudors cards +5 Power until played.
            this.addPattern(new String[]{"~TIME~ if you are losing the round, give your cards ","~NUM~1~"," Power this turn. When returned to your deck, give your ","~CAN~3~"," cards ","~NUM~2~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~3~]'}");

            // (Pythagoras" Theorem) When played, your Common Mega Math cards have +34 Power this turn.
            this.addPattern(new String[]{"~TIME~ your Common ","~CAN~1~"," cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," + // FIXME: RARITY and COLLECTION don't work together...
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Red-eyed Tree Frog) When played, your Opponent's Life on Land cards have -10 Power this turn.
            this.addPattern(new String[]{"~TIME~ your Opponent's ","~CAN~1~"," cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'OTHER','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[]'}");

            // (Kraken) When played, your Oceans & Seas cards have +40 Power this turn.
            this.addPattern(new String[]{"When played, your Oceans & Seas cards have ","~NUM~1~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~A~','CompareTo':'Oceans & Seas'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[Oceans & Seas]'}");

            // (Spitfire) When played, your Dogs and Awesome Aviation cards have +25 Power this turn.
            this.addPattern(new String[]{"~TIME~ your ","~CAN~1~"," and ","~CAN~3~"," cards have ","~NUM~2~"," Power this turn."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND','What':'~CAN~','CompareTo':'~3~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'END_TURN'," +
                            "}]," +
                            "'Combos':'[~1~,~3~]'}");

            // (Matthew Henson) When drawn, give your The North Pole card, even if it's in your deck, +25 Power for the rest of the game.
            this.addPattern(new String[]{"~TIME~ give your ","~CAN~1~"," card, even if it's in your deck, ","~NUM~2~"," Power for the rest of the game."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'PERMANENT'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Catherine Parr) When returned to your deck, give your Turbulent Tudors cards +20 Power and reduce their Energy costs by 1 until played.
            this.addPattern(new String[]{"When returned to your deck, give your ","~CAN~1~"," cards ","~NUM~2~"," Power and reduce their Energy costs by ","~NUM~3~"," until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'POWER','Value':'~2~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~CAN~','CompareTo':'~1~'}," +
                            "'Effect':{'Type':'ENERGY','Value':'-~3~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[~1~]'}");

            // (Thorne-Żytkow Object) When played, give your Constellations, Signs of the Zodiac and Solar System cards +18 Power this turn. If you win the turn, they keep it until they are played again.
            this.addPattern(new String[]{"When played, give your Constellations, Signs of the Zodiac and Solar System cards +18 Power this turn. If you win the turn, they keep it until they are played again."},
                    "{'Effects':[{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Constellations'}," +
                            "'Effect':{'Type':'POWER','Value':'+18'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Signs of the Zodiac'}," +
                            "'Effect':{'Type':'POWER','Value':'+18'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'PLAY'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Solar System'}," +
                            "'Effect':{'Type':'POWER','Value':'+18'}," +
                            "'Duration':'END_TURN'," +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Constellations'}," +
                            "'Effect':{'Type':'POWER','Value':'+18'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Signs of the Zodiac'}," +
                            "'Effect':{'Type':'POWER','Value':'+18'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "},{" +
                            "'TriggerTime':'RETURN'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_DECK','What':'~C~','CompareTo':'Solar System'}," +
                            "'Effect':{'Type':'POWER','Value':'+18'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "'Conditions':[{'Type':'TURN_STATE','Value':'Win'}]" +
                            "}]," +
                            "'Combos':'[Constellations,Signs of the Zodiac,Solar System]'}");

            // (German Shepherd) When drawn, give your cards +10 Power until played.
            this.addPattern(new String[]{"~TIME~ give your cards ","~NUM~1~"," Power until played."},
                    "{'Effects':[{" +
                            "'TriggerTime':'~TIME~'," +
                            "'Target':{'Who':'SELF','Where':'CARDS_IN_HAND'}," +
                            "'Effect':{'Type':'POWER','Value':'~1~'}," +
                            "'Duration':'UNTIL_PLAYED'," +
                            "}]," +
                            "'Combos':'[]'}");

        } catch (Exception e) {
            e.printStackTrace();
            // ","~CAN~~","
            // ","~NUM~~","
            // ","~N_C~~","
        }
    }
}
