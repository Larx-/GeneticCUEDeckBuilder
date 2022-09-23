package Effects;

import EffectConditions.Condition;
import Enums.TriggerTime;
import Enums.Who;
import GameElements.Game;
import GameElements.Player;
import GameElements.Target;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class E_EPT extends Effect {

    Integer changeBy;

    public E_EPT(TriggerTime triggerTime, Target targetPlayer, Integer changeBy, TriggerTime duration, Integer timer, List<Condition> conditions) {
        super(triggerTime, targetPlayer, duration, timer, conditions);

        this.changeBy = changeBy;
    }

    public E_EPT(TriggerTime triggerTime, Target targetPlayer, Integer changeBy, TriggerTime duration, List<Condition> conditions) {
        super(triggerTime, targetPlayer, duration, conditions);

        this.changeBy = changeBy;
    }

    @Override
    public Effect applyEffect(Game game, Who selfPlayer) {
        // 1. Collect Targets
        List<Player> targetPlayers = super.selectPlayers(game, selfPlayer);

        // 2. Check conditions (per card / general)
        if (super.conditionsFulfilled(game, selfPlayer)) {
            // 3. In subclass do effect
            for (Player player : targetPlayers) {
                int newEPT = player.getEnergyPerTurn() + this.changeBy;
                player.setEnergyPerTurn(newEPT);
            }

            // 4. If required return expiryEffect using inverse
            if (super.duration != null) {
                return new E_EPT(super.duration, super.target, (-this.changeBy), null, super.timer, null);
            }
        }
        return null;
    }
}