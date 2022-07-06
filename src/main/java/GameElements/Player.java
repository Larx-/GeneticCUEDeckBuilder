package GameElements;

import Agents.AgentInterface;
import Enums.Where;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class Player {

    @Getter private AgentInterface agent; // Includes the deck
    @Getter @Setter private int energyAvailable;
    @Getter @Setter private int energyPerTurn;
    @Getter @Setter private int powerPerTurn;
    @Getter private boolean doUnlock;

    public Player(AgentInterface agent, int energyAvailable, int energyPerTurn, int powerPerTurn) {
        this.agent = agent;
        this.resetPlayer(energyAvailable, energyPerTurn, powerPerTurn);
    }

    public void resetPlayer(int energyAvailable, int energyPerTurn, int powerPerTurn) {
        this.energyAvailable = energyAvailable;
        this.energyPerTurn = energyPerTurn;
        this.powerPerTurn = powerPerTurn;
        this.doUnlock = false;
        this.getDeck().resetCards();
    }

    public Deck getDeck() {
        return this.agent.getDeck();
    }

    // Includes ALL cards!
    public List<Card> getCardsInDeck() {
        List<Card> cardList = new ArrayList<>(this.agent.getDeck().getCardsInDeck());
        cardList.addAll(this.getCardsInHand());

        return cardList;
    }

    // Includes cards in hand and play!
    public List<Card> getCardsInHand() {
        List<Card> cardList = this.getCardsPlayed();
        cardList.addAll(this.getCardsRemaining());

        return cardList;
    }

    // Only Includes cards in play!
    public List<Card> getCardsPlayed() {
        List<Card> cardList = new ArrayList<>();

        Card[] cardsPlayed = this.agent.getDeck().getCardsPlayed();
        for (int i = 0; i < 3; i++) {
            if (cardsPlayed[i] != null) {
                cardList.add(cardsPlayed[i]);
            }
        }

        return cardList;
    }

    // Only includes cards in hand!
    public List<Card> getCardsRemaining() {
        List<Card> cardList = new ArrayList<>();

        Card[] cardsInHand = this.agent.getDeck().getCardsInHand();
        for (int i = 0; i < 5; i++) {
            if (cardsInHand[i] != null) {
                cardList.add(cardsInHand[i]);
            }
        }

        return cardList;
    }

    public void applyUnlockIfSet() {
        if (this.doUnlock) {
            this.doUnlock = false;
            this.agent.getDeck().unlockCards();
        }
    }

    public void updateDoUnlock() {
        this.doUnlock = this.agent.getDeck().countLockedCards() > 3;
    }

    public void updateEnergyAvailable(int minEnergy, int maxEnergy) {
        this.energyAvailable += this.energyPerTurn;
        this.energyAvailable = Math.max(this.energyAvailable, minEnergy);
        this.energyAvailable = Math.min(this.energyAvailable, maxEnergy);
    }

    public void decideNextTurn() {
        this.energyAvailable = this.agent.decideNextTurn(this.energyAvailable);
    }

    // IDEA: Worth indexing this search?
    public List<Card> findCards (Target target) {
        List<Card> foundCards = new ArrayList<>();
        List<Card> toCheckCards = new ArrayList<>();

        switch (target.getWhere()) {
            case CARDS_IN_DECK:     toCheckCards.addAll(this.getCardsInDeck());     break;
            case CARDS_IN_HAND:     toCheckCards.addAll(this.getCardsInHand());     break;
            case CARDS_REMAINING:   toCheckCards.addAll(this.getCardsRemaining());  break;
        }

        for (Card card : toCheckCards) {
            if (checkCard(card, target)) {
                foundCards.add(card);
            }
        }

        return foundCards;
    }

    private boolean checkCard(Card card, Target compareTo) {
        if (compareTo.getWhat() == null) {
            return true;
        }

        switch (compareTo.getWhat()) {
            case NAME:          return card.getName().equals(compareTo.getName());
            case NAME_INCLUDES: return card.getName().contains(compareTo.getName());
            case COLLECTION:    return card.getCollection() == compareTo.getCollection();
            case ALBUM:         return card.getAlbum() == compareTo.getAlbum();
        }

        return false;
    }
}