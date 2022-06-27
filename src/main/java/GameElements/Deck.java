package GameElements;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

    public List<Card> drawCards() {
        List<Card> draw = new ArrayList<>();
        for (int i = 0; i < 5; i++){
            if (this.cardsInHand[i] == null) {
                this.cardsInHand[i] = this.cardsInDeck.pollFirst();
                draw.add(this.cardsInHand[i]);
            }
        }
        return draw;
    }

    public void burnCards () {
        for (Card card : this.cardsInHand) {
            int power = card.getBurntPower();
            card.setBurntPower(Math.max(0, power - card.getBurnAmount()));
        }
    }

    public void playCard(int indexInHand, int indexOnBoard) {
        this.cardsPlayed[indexOnBoard] = this.cardsInHand[indexInHand];
        this.cardsInHand[indexInHand] = null;
    }

    public int calcPower() {
        int sumPower = 0;
        for (Card c : cardsPlayed) {
            if (c != null) {
                sumPower += c.getModifiedPower();
            }
        }
        return sumPower;
    }

    public void returnPlayedCards() {
        for (int i = 0; i < 3; i++){
            if (this.cardsPlayed[i] != null) {
                this.cardsInDeck.addLast(this.cardsPlayed[i]);
                this.cardsPlayed[i].setBurntPower(this.cardsPlayed[i].getBasePower());
                this.cardsPlayed[i].setBurnAmount(0);
                this.cardsPlayed[i] = null;
            }
        }
    }

    public int countLockedCards() {
        int lockedCards = 0;
        for (int i = 0; i < 5; i++){
            if (this.cardsInHand[i] != null && this.cardsInHand[i].isLocked()) {
                lockedCards++;
            }
        }
        return lockedCards;
    }

    public void unlockCards() {
        for (int i = 0; i < 5; i++){
            if (this.cardsInHand[i] != null && this.cardsInHand[i].isLocked()) {
                this.cardsInHand[i].setLocked(false);
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
