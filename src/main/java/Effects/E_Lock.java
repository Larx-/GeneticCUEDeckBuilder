package Effects;

import Enums.TriggerTime;
import Enums.Who;
import GameElements.Card;
import GameElements.Game;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class E_Lock extends Effect {

    boolean lock;

    public E_Lock(TriggerTime triggerTime, Target targetCards, boolean lock, TriggerTime duration, int timer, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, timer, conditions);

        this.lock = lock;
    }

    public E_Lock(TriggerTime triggerTime, Target targetCards, boolean lock, TriggerTime duration, List<Condition> conditions) {
        super(triggerTime, targetCards, duration, conditions);

        this.lock = lock;
    }

    @Override
    public Effect applyEffect(Game game, Who selfPlayer) {
        // 1. Collect Targets
        List<Card> targetCards = super.selectCards(game,selfPlayer);

        // 2. Check conditions (per card / general)
        if (!targetCards.isEmpty() && super.conditionsFulfilled(game, selfPlayer)) {
            // 3. In subclass do effect
            for (Card card : targetCards) {
                card.setLocked(this.lock);
            }

            // 4. If required return expiryEffect using inverse
            if (super.duration != null && super.duration != TriggerTime.PERMANENT) {
                Target selectedTargetCards = new Target(targetCards);
                return new E_Lock(super.duration, selectedTargetCards, !this.lock,null, super.timer, null); // TODO: this would still unlock a card that was relocked
            }
        }
        return null;
    }
}
