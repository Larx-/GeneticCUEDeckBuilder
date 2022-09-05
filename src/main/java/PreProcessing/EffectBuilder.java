package PreProcessing;

import EffectConditions.*;
import Effects.*;
import Enums.*;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class EffectBuilder {

    private String effect;

    private TriggerTime triggerTime;
    private Target target;
    private int value;
    private TriggerTime duration;
    private int timer;                   // Only when duration == TIMER
    private List<Condition> conditions;

    private Target countEach;            // Only for PowerForEach
    private int upTo;                    // Only for PowerForEach
    private boolean countPlayHistory;    // Only for PowerForEach

    public EffectBuilder () {
        this.conditions = new ArrayList<>();
    }

    public EffectBuilder triggerTime(String triggerTime) {
        this.triggerTime = TriggerTime.fromString(triggerTime);
        return this;
    }

    public EffectBuilder target(String target, String name) {
        this.target = parseTarget(target, name);
        return this;
    }

    public EffectBuilder effect(String effect) {
        int cut = effect.indexOf(",");
        cut = cut == -1 ? effect.length() : cut;
        this.effect = effect.substring(0,cut);

        int idxValue = effect.indexOf("Value:");
        if (idxValue != -1) {
            String valStr = effect.substring(idxValue+6);

            if (valStr.startsWith("(")) {
                log.error("TODO: determination of unsure effect value in brackets");
            } else {
                this.value = Integer.parseInt(valStr);
            }
        }
        return this;
    }

    public EffectBuilder duration(String duration) {
        if (duration.startsWith("TIMER")) {
            this.duration = TriggerTime.TIMER;
            String timerString = "";
            if (duration.contains("Value:")) {
                timerString = duration.substring(duration.indexOf("Value:")+6);
            } else if (duration.contains("Values:")) {
                timerString = duration.substring(duration.indexOf("Values:")+7);
            }

            if (timerString.startsWith("(")) {
                log.error("TODO: determination of unsure timer in brackets");
            } else {
                this.timer = Integer.parseInt(timerString);
            }
        } else {
            this.duration = TriggerTime.fromString(duration);
        }

        return this;
    }

    public EffectBuilder condition(String condition, String name) {
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
            Target t = this.parseTarget(value, name);
            if (t != null) {
                this.conditions.add(new C_PlayedWith(t.getWho(),t.getWhat(),t.getName()));
            }
        } else if (conditionType.equals("PLAYED_BEFORE")) {
            Target t = this.parseTarget(value, name);
            if (t != null) {
                this.conditions.add(new C_PlayedBefore(t.getWho(), t.getWhat(), t.getName()));
            }
        } else if (!value.startsWith("(")) {
            switch (conditionType){
                case "ROUND_STATE":     this.conditions.add(new C_RoundState(this.parseState(value)));      break;
                case "ROUNDS_LOST":     this.conditions.add(new C_RoundsLost(value));                       break;
                case "ROUNDS_WON":      this.conditions.add(new C_RoundsWon(value));                        break;
                case "TURN_STATE":      this.conditions.add(new C_TurnState(value));                        break;
                case "AFTER_ROUND":     this.conditions.add(new C_AfterRound(Integer.parseInt(value)));     break;
                case "BEFORE_ROUND":    this.conditions.add(new C_BeforeRound(Integer.parseInt(value)));    break;
                case "AFTER_TURN":      this.conditions.add(new C_AfterTurn(Integer.parseInt(value)));      break;
                case "BEFORE_TURN":     this.conditions.add(new C_BeforeTurn(Integer.parseInt(value)));     break;
                case "TURN_IN_ROUND":   this.conditions.add(new C_TurnInRound(Integer.parseInt(value)));    break;
                case "DECK_CONTAINS":
                default:
                    log.error("Effect parsing failed during condition determination.");
                    break;
            }
        } else {
            log.error("TODO: determination of condition value in brackets");
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

            default:
                log.error("Effect parsing failed during round state parsing.");
                return null;
        }
    }

    private Target parseTarget(String target, String name) {
        if (target.startsWith("(")) {
            log.error("TODO: determination of unsure targets in brackets");
        }

        if (target.contains("Who:")) {
            this.target = this.parseFullTarget(target);

        } else {
            switch (target) {
                case "THIS":  return new Target(Who.SELF, Where.CARDS_IN_HAND, name, true);
                case "SELF":  return new Target(Who.SELF);
                case "OTHER": return new Target(Who.OTHER);
                default:
                    log.error("Effect parsing failed during target determination.");
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

    public Effect build() {
        this.effect = this.effect == null ? this.effect = "ERROR_EFFECT" : this.effect;

        switch (this.effect) {
            case "POWER_FOR_EACH":  return new E_PowerForEach (this.triggerTime, this.target, this.value, this.duration, this.timer, this.conditions, this.countEach, this.upTo, this.countPlayHistory);
            case "ENERGY_PER_TURN": return new E_EPT    (this.triggerTime, this.target, this.value, this.duration, this.timer, this.conditions);
            case "POWER_PER_TURN":  return new E_PPT    (this.triggerTime, this.target, this.value, this.duration, this.timer, this.conditions);
            case "ENERGY":          return new E_Energy (this.triggerTime, this.target, this.value, this.duration, this.timer, this.conditions);
            case "POWER":           return new E_Power  (this.triggerTime, this.target, this.value, this.duration, this.timer, this.conditions);
            case "BURN":            return new E_Burn   (this.triggerTime, this.target, this.value, this.duration, this.timer, this.conditions);
            case "LOCK":            return new E_Lock   (this.triggerTime, this.target, this.duration, this.timer, this.conditions);
            default:
                log.error("Effect parsing failed during building.");
                return null;
        }
    }
}
