package Effects;

import GameElements.PlayerManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class E_001_EPT extends Effect {

    @Setter List<PlayerManager> effectedPlayers;
    @Getter @Setter TargetPlayers targetPlayers;
    int changeBy;

    public E_001_EPT(List<PlayerManager> effectedPlayers, int changeBy, List<ConditionInterface> conditions) {
        this.effectedPlayers = effectedPlayers;
        this.changeBy = changeBy;
        this.targetPlayers = this.effectedPlayers == null ? TargetPlayers.INVALID_STATE : TargetPlayers.INIT_FINISHED;

        super.conditions = conditions;
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
    public void applyEffect() {
        if (this.effectedPlayers == null) {
            log.error("Effect was not initialized properly!");
        }
        if (super.checkConditions()) {
            for (PlayerManager player : this.effectedPlayers) {
                int newEPT = player.getEnergyPerTurn() + this.changeBy;
                player.setEnergyPerTurn(newEPT);
            }
        }
    }
}