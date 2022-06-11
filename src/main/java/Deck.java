import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.LinkedList;

@Log4j2
public class Deck {

    @Getter private LinkedList<Card> cardsInDeck;
    @Getter private Card[] cardsInHand;
    @Getter private Card[] cardsPlayed;

    public Deck(LinkedList<Card> cards) {
        this.cardsInDeck = cards;
        this.cardsInHand = new Card[5];
        this.cardsPlayed = new Card[3];
    }

    public void shuffleDeck() {
        Collections.shuffle(this.cardsInDeck);
    }

    public void drawCards() {
        for (int i = 0; i < 5; i++){
            if (this.cardsInHand[i] == null) {
                this.cardsInHand[i] = this.cardsInDeck.pollFirst();
            }
        }
    }

    public void playCard(int indexInHand, int indexOnBoard) {
        this.cardsPlayed[indexOnBoard] = this.cardsInHand[indexInHand];
        this.cardsInHand[indexInHand] = null;
    }

    public int executePlay() {
        int sumPower = 0;
        for (Card c : cardsPlayed) {
            if (c != null) {
                sumPower += c.getBasePower();
            }
        }
        return sumPower;
    }

    public void returnPlayedCards() {
        for (int i = 0; i < 3; i++){
            if (this.cardsPlayed[i] != null) {
                this.cardsInDeck.addLast(this.cardsPlayed[i]);
                this.cardsPlayed[i] = null;
            }
        }
    }

    public void printDeck() {
        System.out.print("| ");
        for (Card c : cardsInDeck) {
            System.out.print(c.getName() + " | ");
        }
        System.out.println();
    }
}
