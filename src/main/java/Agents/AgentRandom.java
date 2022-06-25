package Agents;

import Controlling.Main;
import GameElements.Deck;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AgentRandom implements AgentInterface {

    @Getter
    Deck deck;

    public AgentRandom(Deck deck) {
        this.deck = deck;
    }

    @Override
    public int decideNextTurn(int availableEnergy) { // TODO: replace int with something like gameState
        List<Integer> invalidChoices = new ArrayList<>();

        int posToPlay  = 0;
        int usedEnergy = 0;

        while (invalidChoices.size() < 5 && posToPlay <= 2) {
            int cardToPlay = Main.random.nextInt(5);
            if (!invalidChoices.contains(cardToPlay)){
                invalidChoices.add(cardToPlay);

                if (!deck.getCardsInHand()[cardToPlay].isLocked()) {
                    int cardCost = deck.getCardsInHand()[cardToPlay].getModifiedCost();

                    if ((cardCost + usedEnergy) <= availableEnergy) {
                        usedEnergy += cardCost;
                        deck.playCard(cardToPlay,posToPlay);
                        posToPlay++;
                    }
                }
            }
        }
        return availableEnergy - usedEnergy;
    }
}
