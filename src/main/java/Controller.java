import lombok.Getter;

import java.util.*;

public class Controller {

    @Getter List<Card> cardPrototypes;
    private Random random;

    public Controller(Random random) {
        this.random = random;
    }

    // TODO: Read from file(s)
    public void initTestCards (int numToCreate) {
        cardPrototypes = new ArrayList<>();

        for (int i = 0; i < numToCreate; i++) {
            String iStr = ""+i;
            cardPrototypes.add(new Card(iStr, iStr, "Test", i, i));
        }
    }

    public Deck createRandomDeck (int numCards) {
        LinkedList<Card> deckCards = new LinkedList<>();
        Set<String> addedCardsId = new HashSet<>();

        for (int i = 0; i < numCards; i++) {
            Card nextCard;
            do {
                nextCard = cardPrototypes.get(random.nextInt(cardPrototypes.size()));

                if (addedCardsId.contains(nextCard.getId())) {
                    nextCard = null;
                }

            } while (nextCard == null);

            addedCardsId.add(nextCard.getId());
            deckCards.add(nextCard.copy());
        }

        return new Deck(deckCards);
    }
}
