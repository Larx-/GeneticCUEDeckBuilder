package Effects;

import EffectConditions.Condition;
import Enums.TriggerTime;
import Enums.Who;
import GameElements.Card;
import GameElements.Game;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

@Log4j2
public class E_Lock extends Effect {

    public E_Lock(TriggerTime triggerTime, Target targetCards, TriggerTime duration, Integer timer, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, timer, conditions);
    }

    public E_Lock(TriggerTime triggerTime, Target targetCards, TriggerTime duration, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, conditions);
    }

    @Override
    public Effect applyEffect(Game game, Who selfPlayer) {
        // 1. Collect Targets
        List<Card> targetCards = super.selectCards(game,selfPlayer);

        // 2. Check conditions (per card / general)
        if (!targetCards.isEmpty() && super.conditionsFulfilled(game, selfPlayer)) {
            // 3. In subclass do effect
            for (Card card : targetCards) {
                card.addLockDuration(super.duration);

                if (super.duration == TriggerTime.TIMER) {
                    card.addLockTimer(super.timer);
                }
            }
        }
        return null;
    }
}
