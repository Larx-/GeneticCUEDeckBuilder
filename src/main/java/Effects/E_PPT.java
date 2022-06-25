package Effects;

import Enums.TargetPlayers;
import Enums.TriggerTime;
import GameElements.PlayerManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class E_PPT extends Effect {

    @Setter List<PlayerManager> effectedPlayers;
    @Getter @Setter
    TargetPlayers targetPlayers;
    int changeBy;

    public E_PPT(List<PlayerManager> effectedPlayers, int changeBy, List<ConditionInterface> conditions, TriggerTime triggerTime) {
        super(triggerTime);
        super.conditions = conditions;

        this.effectedPlayers = effectedPlayers;
        this.changeBy = changeBy;
        this.targetPlayers = this.effectedPlayers == null ? TargetPlayers.INVALID_STATE : TargetPlayers.INIT_FINISHED;
    }

    public void initialize(PlayerManager self, PlayerManager other) {
        switch (this.targetPlayers) {
            case SELF : this.effectedPlayers.add(self);
                        break;
            case OTHER: this.effectedPlayers.add(other);
                        break;
            case BOTH : this.effectedPlayers.add(self);
                        this.effectedPlayers.add(other);
                        break;
            case INIT_FINISHED: break;
            default:    log.error("Effect initialization is going wrong!");
        }
    }

    @Override
    public Effect applyEffect() {
        if (this.effectedPlayers == null) {
            log.error("Effect was not initialized properly!");
        }
        if (super.conditionsFulfilled()) {
            for (PlayerManager player : this.effectedPlayers) {
                int newPPT = player.getPowerPerTurn() + this.changeBy;
                player.setPowerPerTurn(newPPT);
            }
            return super.expiryEffect;
        }
        return null;
    }
}
