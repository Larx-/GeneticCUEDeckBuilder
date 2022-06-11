import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class AgentPlayer implements AgentInterface {

    @Getter Deck deck;
    Scanner scanner;

    public AgentPlayer(Deck deck) {
        this.deck = deck;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public int getNextTurn(int availableEnergy) { // TODO: replace int with something like gameState
        String inputLine = scanner.nextLine();
        String[] inputStrings = inputLine.split(",");

        int usedEnergy = 0;
        for (int i = 0; i < inputStrings.length; i++) {
            String trimmedInput = inputStrings[i].trim();
            if (!trimmedInput.equals("")){
                int inputInt = Integer.parseInt(trimmedInput);

                if (inputInt >= 0 && inputInt <= 4) {

                    int cardCost = deck.getCardsInHand()[inputInt].getBaseCost();
                    if ((cardCost + usedEnergy) <= availableEnergy) {
                        usedEnergy += cardCost;
                        deck.playCard(inputInt,i);
                    }
                }
            }
        }
        return availableEnergy - usedEnergy;
    }
}
