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
public class E_PowerForEach extends Effect {

    int changeBy;
    Target countEach;

    public E_PowerForEach(TriggerTime triggerTime, Target targetCards, int changeBy, TriggerTime duration, int timer, List<Condition> conditions, Target countEach) {
        super(triggerTime, targetCards, duration, timer, conditions);

        this.changeBy = changeBy;
        this.countEach = countEach;
    }

    public E_PowerForEach(TriggerTime triggerTime, Target targetCards, int changeBy, TriggerTime duration, List<Condition> conditions, Target countEach) {
        super(triggerTime, targetCards, duration, conditions);

        this.changeBy = changeBy;
        this.countEach = countEach;
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
                List<Card> countEachCards = super.selectCards(this.countEach, game, selfPlayer);
                int change = this.changeBy * countEachCards.size();

                for (Card card : targetCards) {
                    int newPower = card.getModifierPower() + change;
                    card.setModifierPower(newPower);
                }

                // 4. If required return expiryEffect using inverse
                if (super.duration != null && super.duration != TriggerTime.PERMANENT) {
                    if (super.duration == TriggerTime.UNTIL_PLAYED) {
                        for (Card card : targetCards) {
                            card.getExpiryEffectsAfterPlayed().add(new E_PowerForEach(super.duration, null, (-change), null, null, countEach));
                        }
                    } else {
                        Target selectedTargetCards = new Target(targetCards);
                        return new E_PowerForEach(super.duration, selectedTargetCards, (-change), null, super.timer, null, countEach);
                    }
                }
            }
        } else {
            log.error("POWER should not be directly applied to players! Use POWER_PER_TURN instead!");
        }
        return null;
    }
}
