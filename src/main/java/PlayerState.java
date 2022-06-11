import lombok.Getter;
import lombok.Setter;

public class PlayerState {

    @Getter private AgentInterface agent; // Includes the deck
    @Getter @Setter private int energyAvailable;
    @Getter @Setter private int energyPerTurn;
    @Getter @Setter private int powerPerTurn;

    public PlayerState(AgentInterface agent, int energyAvailable, int energyPerTurn, int powerPerTurn) {
        this.agent = agent;
        this.energyAvailable = energyAvailable;
        this.energyPerTurn = energyPerTurn;
        this.powerPerTurn = powerPerTurn;
    }

    public Deck getDeck() {
        return this.agent.getDeck();
    }

    public void updateEnergyAvailable(int minEnergy, int maxEnergy) {
        this.energyAvailable += this.energyPerTurn;
        this.energyAvailable = Math.max(this.energyAvailable, minEnergy);
        this.energyAvailable = Math.min(this.energyAvailable, maxEnergy);
    }
}
