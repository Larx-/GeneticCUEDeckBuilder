import java.util.List;

public interface AgentInterface {

    Deck getDeck();
    int getNextTurn(int availableEnergy);
}
