package Effects;

import EffectConditions.Condition;
import Enums.TriggerTime;
import Enums.Who;
import GameElements.Card;
import GameElements.Game;
import GameElements.Target;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class E_Power extends Effect {

    @Getter public Integer changeBy;

    public E_Power(TriggerTime triggerTime, Target targetCards, Integer changeBy, TriggerTime duration, Integer timer, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, timer, conditions);

        this.changeBy = changeBy;
    }

    public E_Power(TriggerTime triggerTime, Target targetCards, Integer changeBy, TriggerTime duration, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, conditions);

        this.changeBy = changeBy;
    }

    @Override
    public Effect applyEffect(Game game, Who selfPlayer) {
        // 0. Are cards meant?
        if (super.target.getWhere() != null || this.target.hasPresetCards()) {
            // 1. Collect Targets
            List<Card> targetCards = super.selectCards(game, selfPlayer);

            // 2. Check conditions (per card / general)
            if (!targetCards.isEmpty() && super.conditionsFulfilled(game, selfPlayer)) {
                // 3. In subclass do effect
                for (Card card : targetCards) {
                    int newPower = card.getModifierPower() + this.changeBy;
                    card.setModifierPower(newPower);
                }

                // 4. If required return expiryEffect using inverse
                if (super.duration != null && super.duration != TriggerTime.PERMANENT) {
                    if (super.duration == TriggerTime.UNTIL_PLAYED) {
                        for (Card card : targetCards) {
                            card.getExpiryEffectsAfterPlayed().add(new E_Power(super.duration, null, (-this.changeBy), null, null));
                        }
                    } else {
                        Target selectedTargetCards = new Target(targetCards);
                        return new E_Power(super.duration, selectedTargetCards, (-this.changeBy), null, super.timer, null);
                    }
                }
            }
        } else {
            log.error("POWER should not be directly applied to players! Use POWER_PER_TURN instead!");
        }
        return null;
    }
}
