package Effects;

import Controlling.Main;
import EffectConditions.Condition;
import Enums.*;
import GameElements.Card;
import GameElements.Game;
import GameElements.Player;
import GameElements.Target;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class Effect {

    @Getter @Setter TriggerTime triggerTime;
    @Getter @Setter Target target;
    @Getter @Setter TriggerTime duration;
    @Getter @Setter Integer timer;
    @Getter @Setter List<Condition> conditions;

    public Effect(TriggerTime triggerTime, Target target, TriggerTime duration, Integer timer, List<Condition> conditions) {
        this.triggerTime = triggerTime;
        this.target = target;
        this.duration = duration;
        this.timer = timer;
        this.conditions = conditions;
    }

    public Effect(TriggerTime triggerTime, Target target, TriggerTime duration, List<Condition> conditions) {
        this.triggerTime = triggerTime;
        this.target = target;
        this.duration = duration;
        this.conditions = conditions;
    }

    public List<Player> selectPlayers(Game game, Who selfPlayer) {
        return this.selectPlayers(this.target, game, selfPlayer);
    }

    public List<Player> selectPlayers(Target t, Game game, Who selfPlayer) {
        List<Player> targetPlayers = new ArrayList<>();

        switch (t.getWho()) {
            case SELF:
                if (selfPlayer == Who.RESIDENT) { targetPlayers.add(game.getResident()); }
                else                            { targetPlayers.add(game.getOpponent()); }
                break;
            case OTHER:
                if (selfPlayer == Who.RESIDENT) { targetPlayers.add(game.getOpponent()); }
                else                            { targetPlayers.add(game.getResident()); }
                break;
            case BOTH:
                targetPlayers.add(game.getResident());
                targetPlayers.add(game.getOpponent());
                break;
        }

        return targetPlayers;
    }

    public List<Card> selectCards(Game game, Who selfPlayer) {
        return this.selectCards(this.target, game, selfPlayer);
    }

    public List<Card> selectCards(Target t, Game game, Who selfPlayer) {
        if (t.hasPresetCards()) {
            return t.getTargetCards();
        }

        List<Player> players = this.selectPlayers(t, game, selfPlayer);
        List<Card> targetCards = players.remove(0).findCards(t);
        if (!players.isEmpty()) {
            targetCards.addAll(players.remove(0).findCards(t));
        }

        if (t.getWhat() == What.RANDOM) {
            List<Card> randCards = new ArrayList<>();

            int i = t.getNumRandCards();
            while (i > 0 && !targetCards.isEmpty()) {
                randCards.add(targetCards.get(Main.random.nextInt(targetCards.size())));
                i--;
            }

            return randCards;
        }

        return targetCards;
    }

    public Effect applyEffect (Game game, Who selfPlayer) {
        log.error("Effect not applied correctly");
        return null;
    }

    protected boolean conditionsFulfilled (Game game, Who selfPlayer) {
        if (this.conditions != null) {
            for (Condition condition : this.conditions) {
                if (!condition.checkConditionFulfilled(game, selfPlayer)) {
                    return false;
                }
            }
        }
        return true;
    }
}

