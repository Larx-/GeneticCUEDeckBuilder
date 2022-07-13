package Setup;

import Effects.*;
import Enums.*;
import Enums.Collection;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;


@Log4j2
public class EffectParser {

    NatLangPatternParser parser;

    public EffectParser (Set<String> cardNames) {
        parser = new NatLangPatternParser(cardNames);
    }

    public String translateEffects (String naturalEffectString, String cardname) {
        try {
            if (naturalEffectString == null || naturalEffectString.equals("") || naturalEffectString.equals("NULL")){
                return "NULL";
            }

            return parser.parseEffect(naturalEffectString, cardname);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<TriggerTime,List<Effect>> parseEffects (String JSONEffectsString) {
        if (JSONEffectsString == null || JSONEffectsString.equals("") || JSONEffectsString.equals("NULL")){
            return null;
        }

        Map<TriggerTime,List<Effect>> effectMap = new HashMap<>();

        JSONObject jsonEffects = new JSONObject(JSONEffectsString);
        JSONArray jsonEffectArray = jsonEffects.getJSONArray("Effects");

        for (int i = 0; i < jsonEffectArray.length(); i++) {
            JSONObject jsonEffect = jsonEffectArray.getJSONObject(i);
            Effect effect = this.parseEffect(jsonEffect);

            if (effect != null) {
                if (!effectMap.containsKey(effect.getTriggerTime())) {
                    List<Effect> effectList = new ArrayList<>();
                    effectList.add(effect);
                    effectMap.put(effect.getTriggerTime(),effectList);
                } else {
                    effectMap.get(effect.getTriggerTime()).add(effect);
                }
            }
        }

        return effectMap;
    }

    public String getCombos (String JSONEffectsString) {
        if (JSONEffectsString == null || JSONEffectsString.equals("") || JSONEffectsString.equals("NULL")) {
            return "[]";
        }
        JSONObject jsonEffects = new JSONObject(JSONEffectsString);
        return jsonEffects.getString("Combos");
    }

    private Effect parseEffect (JSONObject jsonEffect) {
        TriggerTime triggerTime;
        Target target = null;
        TriggerTime duration;
        int timer = -1;
        List<Condition> conditions = null;


        triggerTime = TriggerTime.fromString(jsonEffect.getString("TriggerTime"));


        JSONObject objectTarget = jsonEffect.getJSONObject("Target");

        if (objectTarget.keySet().contains("Who")) {
            Who whoTarget = Who.fromString(objectTarget.getString("Who"));

            if (!objectTarget.keySet().contains("Where")) {
                target = new Target(whoTarget);

            } else {
                Where whereTarget = Where.fromString(objectTarget.getString("Where"));

                if (!objectTarget.keySet().contains("What")) {
                    target = new Target(whoTarget, whereTarget);

                } else {
                    What whatTarget = What.fromString(objectTarget.getString("What"));

                    if (whatTarget == What.RANDOM) { // TODO
                        log.error("Not yet implemented target what: " + whatTarget);

                    } else {
                        String compareToTarget = objectTarget.getString("CompareTo");

                        switch (Objects.requireNonNull(whatTarget)) {
                            case COLLECTION:
                                target = new Target(whoTarget, whereTarget, Collection.fromString(compareToTarget));
                                break;
                            case ALBUM:
                                target = new Target(whoTarget, whereTarget, Album.fromString(compareToTarget));
                                break;
                            case NAME:
                                target = new Target(whoTarget, whereTarget, compareToTarget, true);
                                break;
                            case NAME_INCLUDES:
                                target = new Target(whoTarget, whereTarget, compareToTarget, false);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + whatTarget);
                        }
                    }
                }
            }
        }


        Object objectDuration = jsonEffect.get("Duration");

        if (objectDuration instanceof String) {
            duration = TriggerTime.fromString((String) objectDuration);

        } else {
            String stringDuration = ((JSONObject) objectDuration).getString("Type");
            duration = TriggerTime.fromString(stringDuration);

            if (((JSONObject) objectDuration).keySet().contains("Value")) {
                timer = ((JSONObject) objectDuration).getInt("Value");
            }
        }


        if (jsonEffect.keySet().contains("Conditions")) {
            conditions = new ArrayList<>();
            JSONArray jsonConditionArray = jsonEffect.getJSONArray("Conditions");

            for (int i = 0; i < jsonConditionArray.length(); i++) {
                JSONObject jsonCondition = jsonConditionArray.getJSONObject(i);
                Condition condition = this.parseCondition(jsonCondition);
                if (condition != null) {
                    conditions.add(condition);
                }
            }
        }

        JSONObject effectObject = jsonEffect.getJSONObject("Effect");
        String effectString = effectObject.getString("Type");

        switch (effectString) {
            case "POWER":
                return new E_Power(triggerTime,target,effectObject.getInt("Value"),duration,timer,conditions);
            case "ENERGY":
                return new E_Energy(triggerTime,target,effectObject.getInt("Value"),duration,timer,conditions);
            case "LOCK":
                return new E_Lock(triggerTime,target,true,duration,timer,conditions);
//                log.error("Not yet implemented effect: " + effectString);
            default:
                throw new IllegalStateException("Unexpected value parsing effect: " + effectString);
        }
    }

    private Condition parseCondition(JSONObject jsonCondition) {
        String conditionString = jsonCondition.getString("Type");

        switch (conditionString) {
            case "AFTER_ROUND":
                return new C_AfterRoundX(jsonCondition.getInt("Value"));
            case "BEFORE_ROUND":
                return new C_BeforeRoundX(jsonCondition.getInt("Value"));
            case "PLAYED_WITH":
                return new C_PlayedWith(Who.fromString(jsonCondition.getString("Who")),
                        What.fromString(jsonCondition.getString("What")), jsonCondition.getString("CompareTo"));
            case "PLAYED_BEFORE": // TODO
            case "ROUND_STATE":
            case "TURN_IN_ROUND":
            case "TURN_WON":
            case "TURN_LOST":
            case "ROUNDS_LOST":
            case "ROUNDS_WON":
            case "DECK_CONTAINS":
                log.error("Not yet implemented condition: " + conditionString);
                return null;
            default:
                throw new IllegalStateException("Unexpected value parsing condition: " + conditionString);
        }
    }
}
