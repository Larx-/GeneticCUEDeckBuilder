//package Controlling;
//
//import Effects.*;
//import Enums.*;
//import GameElements.Target;
//import lombok.extern.log4j.Log4j2;
//
//import java.util.List;
//
//@Log4j2
//public class EffectParser {
//
//    public static void main(String[] args) {
//        String example = "TriggerTime:PLAY;Target:CARDS_IN_HAND(SELF,NAME,Mj�lnir),Effect:POWER(+68);Duration:TIMER(0);Condition:PLAYED(SELF,NAME,Thor):PLAYED(SELF,NAME,Thor)";
//        EffectParser effectParser = new EffectParser();
//        System.out.println(effectParser.parseIndividualEffect(example));
//    }
//
//    public Effect parseIndividualEffect (String effectString) {
//        String[] effectComponents = effectString.split(";");
//
//        String[] effectTriggerTime = effectComponents[0].replace("TriggerTime:","").split("(");
//        TriggerTime triggerTime = TriggerTime.fromString(effectTriggerTime[0]); // TODO: super wrong
//        if (effectTriggerTime.length > 1) {
//            String[] params = effectTriggerTime[1].replace(")","").split(",");
//            triggerTime.turns(Integer.parseInt(params[0]));
//        }
//
//        String[] effectTarget = effectComponents[1].replace("Target:","").split("(");
//        Target target = new Target(Where.fromString(effectTarget[0]));
//        if (effectTarget.length > 1) {
//            String[] params = effectTriggerTime[1].replace(")","").split(",");
//            if (params.length > 1) {
//                target.who(Who.fromString(params[0]));
//            }
//            if (params.length > 2) {
//                target.what(What.fromString(params[1]));
//                switch (target.getWhat()) {
//                    case NAME: target.name(params[2]); break;
//                    case COLLECTION: target.collection(Collection.fromString(params[2])); break;
//                    case ALBUM: target.album(Album.fromString(params[2])); break;
//                }
//            }
//        }
//
//        String[] effectDuration = effectComponents[3].replace("Duration:","").split("(");
//        TriggerTime duration = null;
//        if (!effectDuration[0].equals("PERMANENT")) {
//            duration = TriggerTime.fromString(effectTarget[0]);
//            if (effectTarget.length > 1) {
//                String[] params = effectTriggerTime[1].replace(")", "").split(",");
//                duration.turns(Integer.parseInt(params[0]));
//            }
//        }
//
//        List<Condition> conditions = null;
//        if (effectComponents.length > 4) {
//            // TODO: condition parsing
//        }
//
//        Effect effect = null;
//
//        String[] effectEffect = effectComponents[2].replace("Effect:","").split("(");
//        String[] params = effectEffect[1].replace(")","").split(",");
//        switch (effectEffect[0]) {
//            case "Power":
//                if (target == Where.PLAYER) {
//                    effect = new E_PPT(triggerTime,target,Integer.parseInt(params[0]),duration,conditions);
//                } else {
//                    effect = new E_Power(triggerTime,target,Integer.parseInt(params[0]),duration,conditions);
//                }
//                break;
//
//            case "Energy":
//                if (target == Where.PLAYER) {
//                    effect = new E_EPT(triggerTime,target,Integer.parseInt(params[0]),duration,conditions);
//                } else {
//                    effect = new E_Energy(triggerTime,target,Integer.parseInt(params[0]),duration,conditions);
//                }
//                break;
//        }
//
//        return effect;
//    }
//}
