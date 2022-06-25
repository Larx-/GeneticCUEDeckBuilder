package Agents;

import GameElements.Deck;
import lombok.Getter;

import java.util.Scanner;

public class AgentPlayer implements AgentInterface {

    @Getter Deck deck;
    Scanner scanner;

    public AgentPlayer(Deck deck) {
        this.deck = deck;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public int decideNextTurn(int availableEnergy) { // TODO: replace int with something like gameState
        String inputLine = scanner.nextLine();
        String[] inputStrings = inputLine.split(",");

        int usedEnergy = 0;
        for (int i = 0; i < inputStrings.length; i++) {
            String trimmedInput = inputStrings[i].trim();
            if (!trimmedInput.equals("")){
                int inputInt = Integer.parseInt(trimmedInput);

                if (inputInt >= 0 && inputInt <= 4) {

                    if (!deck.getCardsInHand()[inputInt].isLocked()) {

                        int cardCost = deck.getCardsInHand()[inputInt].getModifiedCost();
                        if ((cardCost + usedEnergy) <= availableEnergy) {
                            usedEnergy += cardCost;
                            deck.playCard(inputInt, i);
                        }
                    }
                }
            }
        }
        return availableEnergy - usedEnergy;
    }
}
