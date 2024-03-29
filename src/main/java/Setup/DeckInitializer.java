package Setup;

import Controlling.GenAlg;
import Controlling.Main;
import Effects.*;
import Enums.*;
import Enums.Collection;
import GameElements.Card;
import GameElements.Deck;
import GameElements.Target;
import lombok.Getter;

import java.util.*;

public class DeckInitializer {

    @Getter CardReader cardReader;

    public DeckInitializer (String cardsCSVPath) {
        this.cardReader = new CardReader(cardsCSVPath);
    }

    public Deck createRandomDeck () {
        return this.createRandomDeck(GenAlg.defaultNumCards);
    }

    public Deck createRandomDeck (int numCards) {
        LinkedList<Card> deckCards = new LinkedList<>();
        Set<Integer> addedCardsId = new HashSet<>();

        for (int i = 0; i < numCards; i++) {
            Card nextCard;
            do {
                int cardIndex = Main.random.nextInt(this.cardReader.getNumberOfCards()) + 1;
                nextCard = this.cardReader.getCard(cardIndex);

                if (addedCardsId.contains(nextCard.getId())) {
                    nextCard = null;
                }

            } while (nextCard == null);

            addedCardsId.add(nextCard.getId());
            deckCards.add(nextCard.copyFresh());
        }

        return new Deck(deckCards);
    }

    public Deck createDeckFromCardList (String[] cardIds) {
        LinkedList<Card> deckCards = new LinkedList<>();
        for (String cardId : cardIds) {
            deckCards.add(this.cardReader.getCardByStringIndex(cardId).copyFresh());
        }
        return new Deck(deckCards);
    }

    public Deck createRandomDeckWithCardList (String[] cardIds) {
        LinkedList<Card> deckCards = new LinkedList<>();
        Set<Integer> addedCardsId = new HashSet<>();

        for (String cardId : cardIds) {
            Card nextCard = this.cardReader.getCardByStringIndex(cardId);
            addedCardsId.add(nextCard.getId());
            deckCards.add(nextCard.copyFresh());
        }

        int numberOfCardsToFillRandomly = GenAlg.defaultNumCards - deckCards.size();

        for (int i = 0; i < numberOfCardsToFillRandomly; i++) {
            Card nextCard;
            do {
                int cardIndex = Main.random.nextInt(this.cardReader.getNumberOfCards()) + 1;
                nextCard = this.cardReader.getCard(cardIndex);

                if (addedCardsId.contains(nextCard.getId())) {
                    nextCard = null;
                }

            } while (nextCard == null);

            addedCardsId.add(nextCard.getId());
            deckCards.add(nextCard.copyFresh());
        }

        return new Deck(deckCards);
    }

    public static <T extends Enum<?>> T getRandomEnum(Class<T> clazz) {
        int x = Main.random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }
}
