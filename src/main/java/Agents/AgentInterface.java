package Agents;

import GameElements.Deck;

public interface AgentInterface {

    Deck getDeck();
    int decideNextTurn(int availableEnergy);
}
