package Effects;

import EffectConditions.Condition;
import Enums.TriggerTime;
import Enums.Who;
import GameElements.Card;
import GameElements.Game;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class E_Burn extends Effect {

    Integer burnAmount;

    public E_Burn(TriggerTime triggerTime, Target targetCards, Integer burnAmount, TriggerTime duration, Integer timer, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, timer, conditions);

        this.burnAmount = burnAmount;
    }

    public E_Burn(TriggerTime triggerTime, Target targetCards, Integer burnAmount, TriggerTime duration, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, conditions);

        this.burnAmount = burnAmount;
    }

    @Override
    public Effect applyEffect(Game game, Who selfPlayer) {
        // 1. Collect Targets
        List<Card> targetCards = super.selectCards(game,selfPlayer);

        // 2. Check conditions (per card / general)
        if (!targetCards.isEmpty() && super.conditionsFulfilled(game, selfPlayer)) {
            // 3. In subclass do effect
            for (Card card : targetCards) {
                card.setBurnAmount(card.getBurnAmount() + this.burnAmount);
            }

            // 4. If required return expiryEffect using inverse
            if (super.duration != null && super.duration != TriggerTime.PERMANENT) {
                Target selectedTargetCards = new Target(targetCards);
                return new E_Burn(super.duration, selectedTargetCards, 0, null, super.timer, null);
            }
        }
        return null;
    }
}
