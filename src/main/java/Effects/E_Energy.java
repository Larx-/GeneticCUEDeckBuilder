package Effects;

import EffectConditions.Condition;
import Enums.TriggerTime;
import Enums.Who;
import GameElements.Card;
import GameElements.Game;
import GameElements.Player;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class E_Energy extends Effect {

    Integer changeBy;

    public E_Energy(TriggerTime triggerTime, Target targetCards, Integer changeBy, TriggerTime duration, Integer timer, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, timer, conditions);

        this.changeBy = changeBy;
    }

    public E_Energy(TriggerTime triggerTime, Target targetCards, Integer changeBy, TriggerTime duration, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, conditions);

        this.changeBy = changeBy;
    }

    @Override
    public Effect applyEffect(Game game, Who selfPlayer) {
        // 0. Are cards meant?
        if (super.target.getWhere() != null || this.target.hasPresetCards()) {
            // 1. Collect Targets
            List<Card> targetCards = super.selectCards(game,selfPlayer);
            // 2. Check conditions (per card / general)
            if (!targetCards.isEmpty() && super.conditionsFulfilled(game, selfPlayer)) {
                // 3. In subclass do effect
                for (Card card : targetCards) {
                    int newEnergy = card.getModifierEnergy() + this.changeBy;
                    card.setModifierEnergy(newEnergy);
                }

                // 4. If required return expiryEffect using inverse
                if (super.duration != null && super.duration != TriggerTime.PERMANENT) {
                    if (super.duration == TriggerTime.UNTIL_PLAYED) {
                        for (Card card : targetCards) {
                            card.getExpiryEffectsAfterPlayed().add(new E_Energy(super.duration, null, (-this.changeBy), null, null));
                        }
                    } else {
                        Target selectedTargetCards = new Target(targetCards);
                        return new E_Energy(super.duration, selectedTargetCards, (-this.changeBy), null, super.timer, null);
                    }
                }
            }
        } else {
            // 1. Collect Targets
            List<Player> targetPlayers = super.selectPlayers(game, selfPlayer);

            // 2. Check conditions
            if (super.conditionsFulfilled(game, selfPlayer)) {

                // 3. In subclass do effect
                for (Player player : targetPlayers) {
                    int newEnergy = player.getEnergyAvailable() + this.changeBy;
                    player.setEnergyAvailable(newEnergy);
                }
            }
        }
        return null;
    }
}
