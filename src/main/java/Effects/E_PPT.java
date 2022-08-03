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
public class E_PPT extends Effect {

    int changeBy;

    public E_PPT(TriggerTime triggerTime, Target target, int changeBy, TriggerTime duration, int timer, List<Condition> conditions) {
        super(triggerTime, target, duration, timer, conditions);

        this.changeBy = changeBy;
    }

    public E_PPT(TriggerTime triggerTime, Target target, int changeBy, TriggerTime duration, List<Condition> conditions) {
        super(triggerTime, target, duration, conditions);

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
                int newPPT = player.getPowerPerTurn() + this.changeBy;
                player.setPowerPerTurn(newPPT);
            }

            // 4. If required return expiryEffect using inverse
            if (super.duration != null) {
                return new E_PPT(super.duration, super.target, (-this.changeBy), null, super.timer, null);
            }
        }
        return null;
    }
}
