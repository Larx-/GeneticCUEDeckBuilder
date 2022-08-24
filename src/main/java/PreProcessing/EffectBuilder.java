package PreProcessing;

import EffectConditions.C_RoundState;
import EffectConditions.Condition;
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
        if (target.startsWith("(")) {
            log.error("TODO: determination of unsure targets in brackets");
        }

        if (target.contains("Who:")) {
            Who who = null;
            Where where = null;
            What what = null;
            String compareTo = null;

            int cut = target.indexOf(",");
            String valStr = target.substring(target.indexOf("Who:")+4,cut);
            target = target.substring(cut+1);
            who = Who.fromString(valStr);

            if (target.contains("Where:")) {
                cut = target.indexOf(",");
                valStr = target.substring(target.indexOf("Where:")+6,cut);
                target = target.substring(cut+1);
                where = Where.fromString(valStr);
            }

            if (target.contains("What:")) {
                cut = target.indexOf(",");
                valStr = target.substring(target.indexOf("What:")+5,cut);
                target = target.substring(cut+1);
                what = What.fromString(valStr);
            }

            if (target.contains("CompareTo:")) {
                if (target.contains("(")) {
                    target = target.substring(target.indexOf("(")+1,target.indexOf(")"));

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
                }
                compareTo = target;
            }

            if (where == null && what == null && compareTo == null) {
                this.target = new Target(who);

            } else if (what == null && compareTo == null) {
                this.target = new Target(who, where);

            } else {
                switch (what) {
                    case ALBUM:         this.target = new Target(who, where, Album.fromString(compareTo));      break;
                    case COLLECTION:    this.target = new Target(who, where, Collection.fromString(compareTo)); break;
                    default:            this.target = new Target(who, where, what, compareTo);
                }
            }

        } else {
            switch (target) {
                case "THIS":
                    this.target = new Target(Who.SELF, Where.CARDS_IN_HAND, name, true);
                    break;

                case "SELF":
                    this.target = new Target(Who.SELF);
                    break;

                case "OTHER":
                    this.target = new Target(Who.OTHER);
                    break;

                default:
                    log.error("Effect parsing failed during target determination.");
                    break;
            }
        }
        return this;
    }

    public EffectBuilder effect(String effect) {
        this.effect = effect.substring(0,effect.indexOf(","));

        int idxValue = effect.indexOf("Value:");
        if (idxValue != -1) {
            String valStr = effect.substring(idxValue+6);
            this.value = Integer.parseInt(valStr);
        }
        return this;
    }

    public EffectBuilder duration(String duration) {
        if (duration.startsWith("TIMER")) {
            this.duration = TriggerTime.TIMER;
            String timerString = duration.substring(duration.indexOf("Value:")+6);
            this.timer = Integer.parseInt(timerString);

        } else {
            this.duration = TriggerTime.fromString(duration);
        }

        return this;
    }

    public EffectBuilder condition(String condition) {
        switch (condition.substring(0,condition.indexOf(","))){
            case "ROUND_STATE":
                String state = this.parseState(condition.substring(condition.indexOf("Value:")+6));
                this.conditions.add(new C_RoundState(state));
                break;

            default:
                log.error("Effect parsing failed during condition determination.");
                break;
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

    public Effect build() {
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
