package Setup;

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

    @Getter List<Card> cardPrototypes;

    // TODO: Read from file(s)
    public void initTestCards (int numToCreate) {
        this.cardPrototypes = new ArrayList<>();

        for (int i = 0; i < numToCreate; i++) {
            String iStr = ""+i;
            int cost = Main.random.nextInt(10);
            int power = Math.max(0, (cost * 10) + Main.random.nextInt(40) - 20);
            Collection collection = getRandomEnum(Collection.class);
            Album album = collection.getAffiliatedAlbum();
            String name = collection + " - " + album+" ("+iStr+")";

            Card card = new Card(iStr, name, album, collection, cost, power, this.getRandomEffects());
            this.cardPrototypes.add(card);
        }
    }

    public Map<TriggerTime,List<Effect>> getRandomEffects() {
        if (Main.random.nextInt(100) < 20) {

            List<Condition> condList = new ArrayList<>();
            condList.add(new C_BeforeRoundX(4));

            TriggerTime triggerTime = TriggerTime.PLAY;

            Target target = new Target(Who.SELF, Where.CARDS_IN_DECK, Album.ARTS_AND_CULTURE);

            Effect effect = new E_Power(triggerTime, target, 50, TriggerTime.END_TURN, condList);
            List<Effect> effects = new ArrayList<>();
            effects.add(effect);

            Map<TriggerTime,List<Effect>> map = new HashMap<>();
            map.put(triggerTime,effects);

            return map;
        }
        return null;
    }

    public Deck createRandomDeck (int numCards) {
        LinkedList<Card> deckCards = new LinkedList<>();
        Set<String> addedCardsId = new HashSet<>();

        for (int i = 0; i < numCards; i++) {
            Card nextCard;
            do {
                nextCard = cardPrototypes.get(Main.random.nextInt(cardPrototypes.size()));

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
