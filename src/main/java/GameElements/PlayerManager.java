package GameElements;

import Agents.AgentInterface;
import Enums.Album;
import Enums.Collection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class PlayerManager {

    @Getter private AgentInterface agent; // Includes the deck
    @Getter @Setter private int energyAvailable;
    @Getter @Setter private int energyPerTurn;
    @Getter @Setter private int powerPerTurn;
    @Getter private boolean doUnlock;

    public PlayerManager(AgentInterface agent, int energyAvailable, int energyPerTurn, int powerPerTurn) {
        this.agent = agent;
        this.energyAvailable = energyAvailable;
        this.energyPerTurn = energyPerTurn;
        this.powerPerTurn = powerPerTurn;
        this.doUnlock = false;
    }

    public Deck getDeck() {
        return this.agent.getDeck();
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
    public List<Card> findCards(By by, In in, String search) {
        List<Card> foundCards = new ArrayList<>();

        if (in == In.DECK || in == In.ALL) {
            for (Card card : this.getDeck().getCardsInDeck()) {
                if (checkCard(card, by, search)) {
                    foundCards.add(card);
                }
            }
        }

        if (in == In.HAND_REMAINING || in == In.HAND_PLAY || in == In.ALL) {
            for (Card card : this.getDeck().getCardsInHand()) {
                if (card != null) {
                    if (checkCard(card, by, search)) {
                        foundCards.add(card);
                    }
                }
            }
        }

        if (in == In.PLAY || in == In.HAND_PLAY || in == In.ALL) {
            for (Card card : this.getDeck().getCardsPlayed()) {
                if (card != null) {
                    if (checkCard(card, by, search)) {
                        foundCards.add(card);
                    }
                }
            }
        }

        return foundCards;
    }

    private boolean checkCard(Card card, By by, String search) {
        switch (by) {
            case NAME:          return card.getName().equals(search);
            case NAME_INCLUDES: return card.getName().contains(search);
            case COLLECTION:    return card.getCollection().equalsName(search);
            case ALBUM:         return card/*.getCollection() Maybe someday */.getAlbum().equalsName(search);
        }
        return false;
    }

    // Fills Lists to give boni to and returns false if none are found
    public boolean findCardsForBonus(Collection bC, Album bA, List<Card> bCCards, List<Card> bACards) {
        for (Card card : this.getDeck().getCardsInDeck()) {
            if (null != bC && card.getCollection() == bC) {
                bCCards.add(card);
            } else if (null != bA && card.getAlbum() == bA) {
                bACards.add(card);
            }
        }

        for (Card card : this.getDeck().getCardsInHand()) {
            if (card != null) {
                if (null != bC && card.getCollection() == bC) {
                    bCCards.add(card);
                } else if (null != bA && card.getAlbum() == bA) {
                    bACards.add(card);
                }
            }
        }

        return !bCCards.isEmpty() || !bACards.isEmpty();
    }
}