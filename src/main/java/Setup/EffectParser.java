package Setup;

import Effects.*;
import Enums.*;
import Enums.Collection;
import GameElements.Card;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;


@Log4j2
public class EffectParser {

    String example =
                    "{" +
                    "  'Effects': [{" +
                    "    'TriggerTime': 'PLAY'," +
                    "    'Target': {" +
                    "      'Type': 'CARDS_IN_HAND'," +
                    "      'Params': {" +
                    "        'Who': 'SELF'," +
                    "        'Where': 'CARDS_IN_DECK'," +
                    "        'What': 'NAME'," +
                    "        'CompareTo': 'Mj� lnir'" +
                    "      }" +
                    "    }," +
                    "    'Effect': {" +
                    "      'Type': 'POWER'," +
                    "      'Params': {" +
                    "        'Value': '+68'" +
                    "      }" +
                    "    }," +
                    "    'Duration': {" +
                    "      'Type': 'TIMER'," +
                    "      'Params': {" +
                    "        'Value': '0'" +
                    "      }" +
                    "    }," +
                    "    'Conditions': [{" +
                    "      'Type': 'PLAYED'," +
                    "      'Params': {" +
                    "        'Who': 'SELF'," +
                    "        'What': 'NAME'," +
                    "        'CompareTo': 'Mj� lnir'" +
                    "      }" +
                    "    }, {" +
                    "      'Type': 'BEFORE_ROUND'," +
                    "      'Params': {" +
                    "        'Value': '4'" +
                    "      }" +
                    "    }]" +
                    "" +
                    "  }, {" +
                    "    'TriggerTime': 'RETURN'," +
                    "    'Target': {" +
                    "      'Type': 'CARDS_REMAINING'," +
                    "      'Params': {" +
                    "        'Who': 'OTHER'" +
                    "      }" +
                    "    }," +
                    "    'Effect': {" +
                    "      'Type': 'ENERGY'," +
                    "      'Params': {" +
                    "        'Value': '-1'" +
                    "      }" +
                    "    }," +
                    "    'Duration': 'PERMANENT'" +
                    "  }]" +
                    "}";

    public String translateEffects(String naturalEffectString) {
        // TODO: Matching based translator of effects to JSONstring
        return null;
    }

    public Map<TriggerTime,List<Effect>> parseEffects(String effectsString) {
        if (effectsString == null || effectsString.equals("") || effectsString.equals("-")){
            return null;
        }

        Map<TriggerTime,List<Effect>> effectMap = new HashMap<>();

        JSONObject jsonEffects = new JSONObject(effectsString);
        JSONArray jsonEffectArray = jsonEffects.getJSONArray("Effects");

        for (int i = 0; i < jsonEffectArray.length(); i++) {
            JSONObject jsonEffect = jsonEffectArray.getJSONObject(i);
            Effect effect = this.parseEffect(jsonEffect);

            if (effectMap.containsKey(effect.getTriggerTime())) {
                List<Effect> effectList = new ArrayList<>();
                effectList.add(effect);
                effectMap.put(effect.getTriggerTime(),effectList);
            } else {
                effectMap.get(effect.getTriggerTime()).add(effect);
            }
        }

        return effectMap;
    }

    public Effect parseEffect(JSONObject jsonEffect){
        TriggerTime triggerTime;
        Target target = null;
        TriggerTime duration;
        int timer = -1;
        List<Condition> conditions = null;


        triggerTime = TriggerTime.fromString(jsonEffect.getString("TriggerTime"));


        JSONObject objectTarget = jsonEffect.getJSONObject("Target");
        String stringTarget = objectTarget.getString("Type");
        JSONObject paramsTarget = objectTarget.getJSONObject("Params");

        if (paramsTarget.keySet().contains("Who")) {
            Who whoTarget = Who.fromString(paramsTarget.getString("Who"));

            if (!paramsTarget.keySet().contains("Where")) {
                target = new Target(whoTarget);

            } else {
                Where whereTarget = Where.fromString(paramsTarget.getString("Where"));

                if (!paramsTarget.keySet().contains("What")) {
                    target = new Target(whoTarget, whereTarget);

                } else {
                    What whatTarget = What.fromString(paramsTarget.getString("What"));
                    String compareToTarget = paramsTarget.getString("CompareTo");

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


        Object objectDuration = jsonEffect.get("Duration");

        if (objectDuration instanceof String) {
            duration = TriggerTime.fromString((String) objectDuration);

        } else {
            String stringDuration = ((JSONObject) objectDuration).getString("Type");
            duration = TriggerTime.fromString(stringDuration);

            JSONObject paramsDuration = ((JSONObject) objectDuration).getJSONObject("Params");
            if (paramsDuration.keySet().contains("Value")) {
                timer = paramsDuration.getInt("Value");
            }
        }


        if (jsonEffect.keySet().contains("Conditions")) {
            conditions = new ArrayList<>();
            JSONArray jsonConditionArray = jsonEffect.getJSONArray("Conditions");

            for (int i = 0; i < jsonConditionArray.length(); i++) {
                JSONObject jsonCondition = jsonConditionArray.getJSONObject(i);
                conditions.add(this.parseCondition(jsonCondition));
            }
        }

        JSONObject effectObject = jsonEffect.getJSONObject("Effect");
        String effectString = effectObject.getString("Type");
        JSONObject eParams = effectObject.getJSONObject("Params");

        switch (effectString) {
            case "POWER":
                return new E_Power(triggerTime,target,eParams.getInt("Value"),duration,timer,conditions);
            case "ENERGY":
                return new E_Energy(triggerTime,target,eParams.getInt("Value"),duration,timer,conditions);
            default:
                throw new IllegalStateException("Unexpected value: " + effectString);
        }
    }

    private Condition parseCondition(JSONObject jsonCondition) {
        String conditionString = jsonCondition.getString("Type");
        JSONObject cParams = jsonCondition.getJSONObject("Params");

        switch (conditionString) {
            case "AFTER_ROUND":
                return new C_AfterRoundX(cParams.getInt("Value"));
            case "BEFORE_ROUND":
                return new C_BeforeRoundX(cParams.getInt("Value"));
            case "PLAYED":
                log.error("CONDITION PLAYED NOT IMPLEMENTED");
                return null;
            default:
                throw new IllegalStateException("Unexpected value: " + conditionString);
        }
    }
}
