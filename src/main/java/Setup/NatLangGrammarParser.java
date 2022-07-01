package Setup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NatLangGrammarParser {

    int column;
    Map<Integer,String> triggerTime;
    Map<Integer,String> target;
    Map<Integer,String> effect;
    Map<Integer,String> duration;
    Map<Integer,List<String>> conditions;
    String jsonEffect;

    public static void main(String[] args) {
        NatLangGrammarParser parser = new NatLangGrammarParser();
        boolean b = parser.BEGIN_2("effect.");
        System.out.println(b);
    }

    public String parseEffect(String naturalEffectString) throws Exception {
        column = 0;
        triggerTime = new HashMap<>();
        target = new HashMap<>();
        effect = new HashMap<>();
        duration = new HashMap<>();
        conditions = new HashMap<>();
        jsonEffect = "";

        if (BEGIN(naturalEffectString.toLowerCase())) {
            // TODO: build json effects here
            return jsonEffect;

        }

        throw new Exception("Unexpected value while parsing: " + naturalEffectString);
    }

    private void addCondition (String type, String params) {
        if (!conditions.containsKey(column)) {
            conditions.put(column,new ArrayList<>());
        }
        if (params == null) {
            conditions.get(column).add("{'Type':'"+type+"'}");
        } else {
            conditions.get(column).add("{'Type':'"+type+"','Params':{"+params+"}}");
        }
    }

    private boolean BEGIN (String natStr) {
        if (natStr.equals("")) {
            return true;
        }

        if (natStr.startsWith("when returned to your deck, ")) {
            triggerTime.put(column, "RETURN");
            return BEGIN_2(natStr.substring("when returned to your deck, ".length()));
        }

        if (natStr.startsWith("when played, ")) {
            triggerTime.put(column, "PLAY");
            return BEGIN_2(natStr.substring("when played, ".length()));
        }

        if (natStr.startsWith("when drawn, ")) {
            triggerTime.put(column, "DRAW");
            return BEGIN_2(natStr.substring("when drawn, ".length()));
        }

        if (natStr.startsWith("when drawn or played, ")) {
            triggerTime.put(column, "~DRAW~PLAY");
            return BEGIN_2(natStr.substring("when drawn or played, ".length()));
        }

        // FIXME
        if (natStr.startsWith("when played with ~CARDNAME~, ")) {
            triggerTime.put(column, "PLAY");
            return BEGIN_2(natStr.substring("when played with ~CARDNAME~, ".length()));
        }

        if (natStr.startsWith("when played on the first turn of the round, ")) {
            triggerTime.put(column, "PLAY");
            addCondition("TURN","'Value':'1'");
            return BEGIN_2(natStr.substring("when played on the first turn of the round, ".length()));
        }

        if (natStr.startsWith("at the start of every turn, while in hand, ")) {
            triggerTime.put(column, "START");
            return BEGIN_2(natStr.substring("at the start of every turn, while in hand, ".length()));
        }

        return false;
    }

    private boolean BEGIN_2 (String natStr) {
        int posOfDot = natStr.indexOf(".");
        String natStrEffect = natStr.substring(0, posOfDot);
        String natStrAddition = natStr.substring(posOfDot+1);

        return EFFECT(natStrEffect) && ADDITION(natStrAddition);
    }

    private boolean ADDITION (String natStr) {
        if (natStr.equals("")) {
            return true;
        }

        if (natStr.startsWith(" ")) {
            if (EFFECT(natStr.substring(1))) {
                return true;

            } else {
                column++;
                return BEGIN(natStr.substring(1));
            }
        }

        return false;
    }

    private boolean EFFECT (String natStr) {
        int len = CONDITION(natStr);
        if (len > 0) {
            String subNatStr = natStr.substring(len);
            if (subNatStr.startsWith(", ")) {
                return EFFECT_BODY(subNatStr.substring(", ".length())) != -1;
            }
        }

        int lenEff = EFFECT_BODY(natStr);
        if (lenEff > 0) {
            String subNatStr = natStr.substring(lenEff);
            if (subNatStr.startsWith(" and ")) {
                return EFFECT_BODY(subNatStr.substring(" and ".length())) != -1;

            } else if (subNatStr.startsWith(", and ")) {
                return EFFECT_BODY(subNatStr.substring(", and ".length())) != -1;

            } else {
                return true;
            }
        }

        return false;
    }

    private int EFFECT_BODY (String natStr) {

        if (natStr.startsWith("lock ")) {

        }

//        "lock ~TARGET~ ~TIME~"
//        "gain ~NUMBER~ power ~TIME~"
//        "gain ~NUMBER~ power/turn ~TIME~"
//        "give ~TARGET~ ~NUMBER~ power ~TIME~"
//        "give ~TARGET~ an extra ~NUMBER~ power ~TIME~"
//        "also give ~TARGET~ ~NUMBER~ power ~TIME~"
//        "reduce the energy cost of ~TARGET~ by ~NUMBER~ ~TIME~"
//        "increase the energy cost of ~TARGET~ by ~NUMBER~ ~TIME~"
//        "this card steals ~NUMBER~ power from ~TARGET~ and keeps it ~TIME~"
//        "~TARGET~ cost ~NUMBER~ more energy ~TIME~"
//        "~TARGET~ cost ~NUMBER~ less energy ~TIME~"
//        "~TARGET~ have ~NUMBER~ power ~TIME~"
//        "~TARGET~ gain ~NUMBER~ power ~TIME~"
//        "~TARGET~ gain ~NUMBER~ energy"
//        "~TARGET~ lose ~NUMBER~ power ~TIME~"

        return -1;
    }

    private boolean TIME (String natStr) {

//        ""                      -> this turn
//        "for ~TIME_BODY~"
//        "~TIME_BODY~"

        return false;
    }

    private boolean TIME_BODY (String natStr) {

//        "next turn"
//        "this turn"
//        "until played"
//        "until it is played"
//        "this round"
//        "the rest of the round"
//        "~NUMBER~ turns"
//        "until the end of the round"
//        "this turn and next"
//        "the rest of the game"

        return false;
    }

    private boolean TARGET (String natStr) {

//        "~TARGET_BODY~~IRRELEVANT_PARENTHESIS~"

        return false;
    }

    private boolean TARGET_BODY (String natStr) {

//        "both players"
//        "this card"
//        "it"
//        "that card"
//        "them"
//        "all cards"
//        "all adjacent and opposite cards"
//        "all of your cards with ~NUMBER~ or less base energy"
//        "each of your opponent�s cards"
//        "a random card in your hand"
//        "a random card in your opponent's hand"
//        "a random ~COLLECTION~ card and a random ~ALBUM~ card in your hand"
//        "a random ~COLLECTION~ card in your hand"
//        "your opponent's cards left in hand"
//        "your opponent's cards"
//        "your cards"
//        "your cards in hand"
//        "your ~ALBUM~ cards"
//        "your ~ALBUM~ cards in hand"
//        "your ~ALBUM~ and ~ALBUM~ cards"
//        "your ~CARDNAME~ card in hand"
//        "~NUMBER~ random cards (in either player's hand)"
//        "~CARDNAME~ and your common and rare cards"
//        "~CARDNAME~ cards"
//        "~CARDNAME~ or ~CARDNAME~"
//        "~CARDNAME~ and ~CARDNAME~"
//        "~CARDNAME~"

        return false;
    }

    private int IRRELEVANT_PARENTHESIS (String natStr) {
        if (natStr.startsWith(" (even if they're in your deck)")) {
            return " (even if they're in your deck)".length();
        }
        if (natStr.startsWith(" (wherever they are)")) {
            return " (wherever they are)".length();
        }
        if (natStr.startsWith(" (even if it's in your deck)")) {
            return " (even if it's in your deck)".length();
        }
        return 0;
    }

    private int CONDITION (String natStr) {

//        "if you won the turn"
//        "If you are losing the round"
//        "if it contains cards from ~NUMBER~ or more albums"
//        "if you lost this turn"
//        "if you have played ~CARDNAME~"
//        "if you have played ~CARDNAME~ this game"
//        "if you have played ~CARDNAME~, ~CARDNAME~ and ~CARDNAME~"
//        "if your deck contains ~NUMBER~ or more ~ALBUM~ cards"
//        "if your deck contains ~CARDNAME~"
//        "if you have lost ~NUMBER~ rounds of this game"

        return -1;
    }

    private boolean NUMBER (String natStr) {
        return false;
    }

    private boolean CARDNAME (String natStr) {
        return false;
    }

    private boolean ALBUM (String natStr) {
        return false;
    }

    private boolean COLLECTION (String natStr) {
        return false;
    }
}
