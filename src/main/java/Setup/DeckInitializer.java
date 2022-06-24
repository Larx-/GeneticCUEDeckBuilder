package Setup;

import Controlling.Main;
import Effects.*;
import GameElements.Card;
import GameElements.Deck;
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
            Album album = getRandomEnum(Album.class);
            Collection collection = getRandomEnum(Collection.class);
            String name = collection + " - " + album+" ("+iStr+")";

            Card card = new Card(iStr, name, album, collection, cost, power, getFullRandomEffect());
            this.cardPrototypes.add(card);
        }
    }

    public EffectCollection getFullRandomEffect() {
        if (Main.random.nextInt(100) < 20) { // 10% chance for any card to have this effect
            String desc = "";
            // On PLAY: If you are losing the round, and it's after round 2 give all A&C cards in your deck +50 until the end of the round
            List<ConditionInterface> cond_list   = new ArrayList<>();
            cond_list.add(new C_010_RoundStatus(-2));
            desc += "Con: loosing round";
            cond_list.add(new C_001_AfterRoundX(2));
            desc += " & after round 2";

            E_030_Power effect_Plus50 = new E_030_Power(null, 50, cond_list);
            effect_Plus50.setInitializationString(TargetCards.OWN + Main.SEPARATOR +
                    TargetQualifiers.FROM_ALBUM        + Main.SEPARATOR + Album.ARTS_AND_CULTURE + Main.SEPARATOR +
                    TargetQualifiers.BASE_ENERGY_UNDER + Main.SEPARATOR + "6");
            desc += ", Eff: +50 to own A&C";

            E_030_Power effect_Minus50 = new E_030_Power(null, -50, null);

            EffectContainer effectContainer_Plus50 = new EffectContainer(effect_Plus50, TriggerTime.ON_PLAY);
            desc = "ON PLAY   " + desc;
            effectContainer_Plus50.setExpiryEffect(new EffectContainer(effect_Minus50, TriggerTime.ON_END_ROUND));
            desc += ", Dur: end of round";

            return new EffectCollection(desc,false, effectContainer_Plus50);
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
