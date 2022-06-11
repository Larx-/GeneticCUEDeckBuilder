import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AgentRandom implements AgentInterface {

    @Getter Deck deck;
    Random random;

    public AgentRandom(Deck deck, Random random) {
        this.deck = deck;
        this.random = random;
    }

    @Override
    public int getNextTurn(int availableEnergy) { // TODO: replace int with something like gameState
        List<Integer> invalidChoices = new ArrayList<>();

        int posToPlay  = 0;
        int usedEnergy = 0;

        while (invalidChoices.size() < 5 && posToPlay <= 2) {
            int cardToPlay = random.nextInt(5);
            if (!invalidChoices.contains(cardToPlay)){
                invalidChoices.add(cardToPlay);
                int cardCost = deck.getCardsInHand()[cardToPlay].getBaseCost();

                if ((cardCost + usedEnergy) <= availableEnergy) {
                    usedEnergy += cardCost;
                    deck.playCard(cardToPlay,posToPlay);
                    posToPlay++;
                }
            }
        }
        return availableEnergy - usedEnergy;
    }
}
