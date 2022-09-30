package PreProcessing;

import EffectConditions.*;
import Effects.*;
import Enums.*;
import Enums.Collection;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;

import java.util.*;

@Log4j2
public class EffectBuilder {

    private int subEffectCounter;
    private String cardName;
    private Set<String> cardNames;

    private Map<Integer,String> effect;
    private Map<Integer,TriggerTime>   triggerTime;
    private Map<Integer,Target>        target;
    private Map<Integer,Integer>       value;
    private Map<Integer,TriggerTime>   duration;
    private Map<Integer,Integer>       timer;                  // Only when duration == TIMER
    private Map<Integer,List<Condition>> conditions;

    private Map<Integer,Target>        countEach;              // Only for PowerForEach
    private Map<Integer,Integer>       upTo;                   // Only for PowerForEach
    private Map<Integer,Boolean>       countPlayHistory;       // Only for PowerForEach

    public EffectBuilder (Set<String> cardNames, String cardName) {
        this.subEffectCounter = 0;
        this.cardNames = cardNames;
        this.cardName = cardName;

        this.effect           = new HashMap<>();
        this.triggerTime      = new HashMap<>();
        this.target           = new HashMap<>();
        this.value            = new HashMap<>();
        this.duration         = new HashMap<>();
        this.timer            = new HashMap<>();
        this.conditions       = new HashMap<>();
        this.conditions.put(0,new ArrayList<>());

        this.countEach        = new HashMap<>();
        this.upTo             = new HashMap<>();
        this.countPlayHistory = new HashMap<>();
    }

    public EffectBuilder addChunk (String chunkType, String chunkParam) {
        switch (chunkType){
            case "TriggerTime": return this.triggerTime(chunkParam);
            case "Condition":   return this.condition(chunkParam);
            case "Target":      return this.target(chunkParam);
            case "Effect":      return this.effect(chunkParam);
            case "Duration":    return this.duration(chunkParam);
            case "Repeat":
            default:
                //log.error("Parsing failed during chunk type determination.");
                return this;
        }
    }

    public EffectBuilder triggerTime(String triggerTime) {
        if (this.triggerTime.get(this.subEffectCounter) != null) { this.subEffectCounter++; }
        this.triggerTime.put(this.subEffectCounter, TriggerTime.fromString(triggerTime));
        return this;
    }

    public EffectBuilder target(String target) {
        if (this.target.get(this.subEffectCounter) != null) { this.subEffectCounter++; }
        this.target.put(this.subEffectCounter, parseTarget(target));
        return this;
    }

    public EffectBuilder effect(String effect) {
        if (this.effect.get(this.subEffectCounter) != null) { this.subEffectCounter++; }

        int cut = effect.indexOf(",");
        cut = cut == -1 ? effect.length() : cut;

        this.effect.put(this.subEffectCounter, effect.substring(0,cut));

        int idxValue = effect.indexOf("Value:");
        if (idxValue != -1) {
            String valStr = effect.substring(idxValue+6);

            if (valStr.startsWith("(")) {
                Object o = this.parseBrackets(valStr);
                if (o != null && o.getClass() == Integer.class) {
                    this.value.put(this.subEffectCounter, (Integer) o);
                } else {
                    log.error("TODO: determination of unsure effect value in brackets: " + valStr);
                }
            } else {
                this.value.put(this.subEffectCounter, Integer.parseInt(valStr));
            }
        }
        return this;
    }

    public EffectBuilder duration(String duration) {
        if (this.duration.get(this.subEffectCounter) != null) { this.subEffectCounter++; }

        if (duration.startsWith("TIMER")) {
            this.duration.put(this.subEffectCounter, TriggerTime.TIMER);
            String timerString = "";
            if (duration.contains("Value:")) {
                timerString = duration.substring(duration.indexOf("Value:")+6);
            } else if (duration.contains("Values:")) {
                timerString = duration.substring(duration.indexOf("Values:")+7);
            }

            if (timerString.startsWith("(")) {
                Object o = this.parseBrackets(timerString);
                if (o != null) {
                    if (o.getClass() == Integer.class) {
                        this.duration.put(this.subEffectCounter, TriggerTime.TIMER);
                        this.timer.put(this.subEffectCounter, (Integer) o);
                    } else if (o.getClass() == TriggerTime.class) {
                        this.duration.put(this.subEffectCounter, (TriggerTime) o);
                    }
                } else {
                    log.error("TODO: determination of unsure timer in brackets: " + timerString);
                }
            } else {
                this.timer.put(this.subEffectCounter, Integer.parseInt(timerString));
            }
        } else {
            this.duration.put(this.subEffectCounter, TriggerTime.fromString(duration));
        }

        return this;
    }

    public EffectBuilder condition(String condition) {
        this.conditions.computeIfAbsent(this.subEffectCounter, k -> new ArrayList<>());

        int cut = condition.indexOf(",");
        cut = cut == -1 ? condition.length() : cut;
        String conditionType = condition.substring(0,cut);
        String value = "";
        if (condition.contains("Value:")) {
            value = condition.substring(condition.indexOf("Value:")+6);
        } else {
            value = condition.substring(condition.indexOf(",")+1);
        }

        if (conditionType.equals("PLAYED_WITH")) {
            Target t = this.parseTarget(value);
            if (t != null) {
                this.conditions.get(this.subEffectCounter).add(new C_PlayedWith(t.getWho(),t.getWhat(),t.getName()));
            }
        } else if (conditionType.equals("PLAYED_BEFORE")) {
            Target t = this.parseTarget(value);
            if (t != null) {
                this.conditions.get(this.subEffectCounter).add(new C_PlayedBefore(t.getWho(), t.getWhat(), t.getName()));
            }
        } else {
            if (value.startsWith("(")) {
                Object o = this.parseBrackets(value);
                if (o != null) {
                    value = String.valueOf(o);
                } else {
                    log.error("TODO: determination of condition value in brackets: " + value);
                }
            }

            switch (conditionType){
                case "ROUND_STATE":     this.conditions.get(this.subEffectCounter).add(new C_RoundState(this.parseState(value)));      break;
                case "ROUNDS_LOST":     this.conditions.get(this.subEffectCounter).add(new C_RoundsLost(value));                       break;
                case "ROUNDS_WON":      this.conditions.get(this.subEffectCounter).add(new C_RoundsWon(value));                        break;
                case "TURN_STATE":      this.conditions.get(this.subEffectCounter).add(new C_TurnState(value));                        break;
                case "AFTER_ROUND":     this.conditions.get(this.subEffectCounter).add(new C_AfterRound(Integer.parseInt(value)));     break;
                case "BEFORE_ROUND":    this.conditions.get(this.subEffectCounter).add(new C_BeforeRound(Integer.parseInt(value)));    break;
                case "AFTER_TURN":      this.conditions.get(this.subEffectCounter).add(new C_AfterTurn(Integer.parseInt(value)));      break;
                case "BEFORE_TURN":     this.conditions.get(this.subEffectCounter).add(new C_BeforeTurn(Integer.parseInt(value)));     break;
                case "TURN_IN_ROUND":   this.conditions.get(this.subEffectCounter).add(new C_TurnInRound(Integer.parseInt(value)));    break;
                case "DECK_CONTAINS":
                default:
                    //log.error("Effect parsing failed during condition determination.");
                    break;
            }
        }

        return this;
    }

    private String parseState(String state) {
        switch (state) {
            case "Win":
            case "EqWin":
            case "Equal":
            case "EqLoss":
            case "Loss":
                return state;

            case "won":
            case "win":
            case "winning":
                return "Win";

            case "winning or drawing":
            case "not losing":
                return "EqWin";

            case "not winning":
                return "EqLoss";

            case "lose":
            case "lost":
            case "losing":
                return "Loss";

            default:
                //log.error("Effect parsing failed during round state parsing.");
                return null;
        }
    }

    private Target parseTarget(String target) {
        if (target.startsWith("(")) {
            Object o = this.parseBrackets(target);
            if (o != null) {
                if (o.getClass() == Target.class) {
                    return (Target) o;
                } else if (o.getClass() == ArrayList.class) {
                    return ((ArrayList<Target>) o).get(0); // TODO: MULTI-TARGETS!!!
                }
            } else {
                log.error("TODO: determination of unsure targets in brackets: " + target);
            }
        }

        if (target.contains("Who:")) {
            return this.parseFullTarget(target);

        } else {
            switch (target) {
                case "THIS":  return new Target(Who.SELF, Where.CARDS_IN_HAND, this.cardName, true);
                case "SELF":  return new Target(Who.SELF);
                case "OTHER": return new Target(Who.OTHER);
                default:
                    //log.error("Effect parsing failed during target determination.");
                    break;
            }
        }

        return null;
    }

    private Target parseFullTarget(String target) {
        Who who = null;
        Where where = null;
        What what = null;
        String compareTo = null;

        int cut = target.indexOf(",");
        String valStr;
        if (cut == -1) {
            valStr = target.substring(target.indexOf("Who:")+4);
        } else {
            valStr = target.substring(target.indexOf("Who:")+4,cut);
            target = target.substring(cut+1);
        }
        who = Who.fromString(valStr);

        if (target.contains("Where:")) {
            cut = target.indexOf(",");
            if (cut == -1) {
                valStr = target.substring(target.indexOf("Where:")+6);
            } else {
                valStr = target.substring(target.indexOf("Where:")+6,cut);
                target = target.substring(cut+1);
            }
            where = Where.fromString(valStr);
        }

        if (target.contains("What:")) {
            cut = target.indexOf(",");
            if (cut == -1) {
                valStr = target.substring(target.indexOf("What:")+5);
            } else {
                valStr = target.substring(target.indexOf("What:")+5,cut);
                target = target.substring(cut+1);
            }
            what = What.fromString(valStr);
        }

        if (target.contains("CompareTo:")) {
            if (target.contains("(")) {
                target = target.substring(target.indexOf("(") + 1, target.indexOf(")"));
            } else {
                target = target.substring(target.indexOf("CompareTo:") + 10);
            }

            if (what == null) {
                if (Album.fromString(target) != null) {
                    what = What.ALBUM;
                } else if (Collection.fromString(target) != null) {
                    what = What.COLLECTION;
                } else {
                    what = What.NAME; // TODO: others could be possible
                }
            } else {
                log.error("Something is not adding up with a fuzzy target, but with set What.");
            }
            compareTo = target;
        }

        if (where == null && what == null && compareTo == null) {
            return new Target(who);

        } else if (what == null && compareTo == null) {
            return new Target(who, where);

        } else {
            switch (what) {
                case ALBUM:         return new Target(who, where, Album.fromString(compareTo));
                case COLLECTION:    return new Target(who, where, Collection.fromString(compareTo));
                default:            return new Target(who, where, what, compareTo);
            }
        }
    }

    private Object parseBrackets(String bracketText) {
        // Cleanup TODO: Regex refactor
        bracketText = bracketText.replace("(","").replace(")","");
        bracketText = bracketText.replace(", wherever they are,", "").replace(", wherever they are", "").replace("wherever they are", "");
        bracketText = bracketText.replace(", even if they're in your deck,","").replace(", even if it's in your deck,","").trim();
        if (bracketText.endsWith("+")) { bracketText = bracketText.substring(0,bracketText.length() - 1).trim(); }

        // Find pure integers
        try { return Integer.parseInt(bracketText); }
        catch (NumberFormatException ignored) { }

        // Default params for targets
        Who who = Who.SELF;
        Where where = Where.CARDS_IN_DECK;

        // TODO: Pointers to previously mentioned cards
        if (bracketText.equals("it") || bracketText.equals("their") || bracketText.equals("that card") ||
                bracketText.equals("it and this card") || bracketText.equals("them")) {
             return new Target(Who.SELF, Where.CARDS_IN_HAND, Collection.NAN); // TODO: Placeholder non-target
        }

        // Filter specific phrases that would mess up the next step
        if (bracketText.equals("all other cards"))       { return new Target(Who.BOTH, Where.CARDS_IN_DECK); }  // TODO: don't include this card
        if (bracketText.equals("your cards"))            { return new Target(Who.SELF, Where.CARDS_IN_DECK); }
        if (bracketText.equals("all your cards"))        { return new Target(Who.SELF, Where.CARDS_IN_DECK); }
        if (bracketText.equals("all of your cards"))     { return new Target(Who.SELF, Where.CARDS_IN_DECK); }
        if (bracketText.equals("your Opponent"))         { return new Target(Who.OTHER); }
        if (bracketText.equals("your Opponent's cards")) { return new Target(Who.OTHER, Where.CARDS_IN_DECK); }
        if (bracketText.equals("Opponent's"))            { return new Target(Who.OTHER, Where.CARDS_IN_DECK); }
        if (bracketText.equals("your other cards"))      { return new Target(Who.SELF, Where.CARDS_IN_DECK); }  // TODO: don't include this card
        if (bracketText.equals("your remaining cards in hand")) { return new Target(Who.OTHER, Where.CARDS_REMAINING); } // TODO: don't include this card
        if (bracketText.equals("this") || bracketText.equals("this card's") || bracketText.equals("this card") || bracketText.equals("this cards")
                || bracketText.equals("this card in hand")) { return new Target(Who.SELF, Where.CARDS_IN_HAND, this.cardName, true); }

        // Remove starting words like "your" / "any" and set corresponding params
        if (bracketText.startsWith("your")) {
            bracketText = bracketText.replaceFirst("your", "").trim();

            if (bracketText.startsWith("Opponent")) {
                who = Who.OTHER;
                bracketText = bracketText.replaceFirst("Opponents", "").replaceFirst("Opponent's", "").trim();
            }
        }
        if (bracketText.startsWith("any")) {
            who = Who.BOTH;
            bracketText = bracketText.replaceFirst("any", "").trim();
        }

        // Remove ending words like "card(s)" / "in your (Opponent's) hand" and set corresponding params
        if (bracketText.endsWith("in your hand")) {
            where = Where.CARDS_IN_HAND;
            bracketText = bracketText.substring(0, bracketText.lastIndexOf("in your hand")).trim();
        }
        if (bracketText.endsWith("in your Opponent's hand")) {
            who = Who.OTHER;
            where = Where.CARDS_IN_HAND;
            bracketText = bracketText.substring(0, bracketText.lastIndexOf("in your Opponent's hand")).trim();
        }
        if (bracketText.endsWith("in your Opponent's deck and hand")) {
            who = Who.OTHER;
            where = Where.CARDS_IN_DECK;
            bracketText = bracketText.substring(0, bracketText.lastIndexOf("in your Opponent's deck and hand")).trim();
        }
        if (bracketText.endsWith("card") || bracketText.endsWith("cards")) {
            bracketText = bracketText.substring(0, bracketText.lastIndexOf(" card")).trim();
        }

        // Split multi target phrases and for now only consider the first one
        if (bracketText.contains(" and ")) {
            String[] splitBracketText = bracketText.split(", | and ");
            bracketText = splitBracketText[0];
        }
        // TODO: Multi-Target from HERE

        if (bracketText.equals("a random"))           { return new Target(who, where, 1); }
        if (bracketText.equals("two random"))         { return new Target(who, where, 2); }
        if (bracketText.equals("3 random"))           { return new Target(who, where, 3); }
        if (bracketText.equals("4 random"))           { return new Target(who, where, 4); }
        if (bracketText.equals("a random card left")) { return new Target(who, Where.CARDS_REMAINING, 1); }

        if (bracketText.equals("cards in hand"))      { return new Target(who, Where.CARDS_IN_HAND); }
        if (bracketText.equals("all"))                { return new Target(who, where); }
        if (bracketText.equals("adjacent"))           { return new Target(who, Where.CARDS_PLAYED, 2); } // TODO: fix workaround

        // Find pure collections
        Collection collection = Collection.fromString(bracketText);
        if (collection != null) { return new Target(who, where, collection); }

        // Find pure albums
        Album album = Album.fromString(bracketText);
        if (album != null) { return new Target(who, where, album); }

        // Find pure cardNames
        if (this.cardNames.contains(bracketText.toLowerCase())) {
            return new Target(who, where, bracketText, true);
        }

        // TODO: to HERE

        // Find win / loss sates
        String state = this.parseState(bracketText);
        if (state != null) { return state; }

        // Tedious replacement
        if (bracketText.equals("first"))    { return 1; }
        if (bracketText.equals("second"))   { return 2; }
        if (bracketText.equals("two"))      { return 2; }
        if (bracketText.equals("third"))    { return 3; }
        if (bracketText.equals("three"))    { return 3; }
        if (bracketText.equals("last"))     { return 3; }

        if (bracketText.equals("at least one")) { return ">=1"; }
        if (bracketText.equals("at least 1"))   { return ">=1"; }
        if (bracketText.equals("one or more"))  { return ">=1"; }
        if (bracketText.equals("two or more"))  { return ">=2"; }

//        log.debug(bracketText);
        if (EffectChunkParser.fuzzyBracketUnknowns.containsKey(bracketText)) {
            EffectChunkParser.fuzzyBracketUnknowns.put(bracketText, EffectChunkParser.fuzzyBracketUnknowns.get(bracketText) + 1);
        } else {
            EffectChunkParser.fuzzyBracketUnknowns.put(bracketText, 1);
        }
        return new Target(Who.SELF, Where.CARDS_IN_HAND, Collection.NAN); // TODO: Placeholder non-target
    }

    public List<Effect> build() {
        List<Effect> effectList = new ArrayList<>();

        for (int i = 0; i <= this.subEffectCounter; i++) {
            // TODO: Propagate values forward and back correctly
            this.effect.putIfAbsent(i, "ERROR_EFFECT");

            switch (this.effect.get(i)) {
                case "POWER_FOR_EACH":  effectList.add(new E_PowerForEach (this.triggerTime.get(i), this.target.get(i), this.value.get(i), this.duration.get(i), this.timer.get(i), this.conditions.get(i), this.countEach.get(i), this.upTo.get(i), this.countPlayHistory.get(i))); break;
                case "ENERGY_PER_TURN": effectList.add(new E_EPT    (this.triggerTime.get(i), this.target.get(i), this.value.get(i), this.duration.get(i), this.timer.get(i), this.conditions.get(i))); break;
                case "POWER_PER_TURN":  effectList.add(new E_PPT    (this.triggerTime.get(i), this.target.get(i), this.value.get(i), this.duration.get(i), this.timer.get(i), this.conditions.get(i))); break;
                case "ENERGY":          effectList.add(new E_Energy (this.triggerTime.get(i), this.target.get(i), this.value.get(i), this.duration.get(i), this.timer.get(i), this.conditions.get(i))); break;
                case "POWER":           effectList.add(new E_Power  (this.triggerTime.get(i), this.target.get(i), this.value.get(i), this.duration.get(i), this.timer.get(i), this.conditions.get(i))); break;
                case "BURN":            effectList.add(new E_Burn   (this.triggerTime.get(i), this.target.get(i), this.value.get(i), this.duration.get(i), this.timer.get(i), this.conditions.get(i))); break;
                case "LOCK":            effectList.add(new E_Lock   (this.triggerTime.get(i), this.target.get(i), this.duration.get(i), this.timer.get(i), this.conditions.get(i))); break;
                default:                //log.error("Effect parsing failed during building.");
            }
        }

        return effectList;
    }
}
