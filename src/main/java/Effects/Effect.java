package Effects;

import Controlling.Main;
import GameElements.Card;
import GameElements.Game;
import GameElements.PlayerManager;
import Setup.Album;
import Setup.Collection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class Effect {

    @Getter List<ConditionInterface> conditions = null;

    public void applyEffect () {
       log.error("Parent effect should not be called!"); // FIXME: Move the upper part into here?
    }

    protected boolean checkConditions () {
        if (this.conditions != null) {
            for (ConditionInterface condition : this.conditions) {
                if (!condition.checkConditionFulfilled()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setEffectedCards (List<Card> cards) {

    }

    public String getInitializationString() {
        return null;
    }

    public static List<Card> initializeEffectedCards (String initString, Game game, boolean isOpponentView) {
        String[] initStrings = initString.split(Main.SEPARATOR);
        List<Card> targetedCards = new ArrayList<>();

        TargetCards initTargetCards = TargetCards.fromString(initStrings[0]);
        if (initTargetCards != null) {
            PlayerManager self  = isOpponentView ? game.getOpponent() : game.getResident();
            PlayerManager other = isOpponentView ? game.getResident() : game.getOpponent();

            switch (initTargetCards) {
                case OWN             : targetedCards.addAll(self.getDeck().getCardsInDeck());
                case OWN_HAND        : targetedCards.addAll(Arrays.asList(self.getDeck().getCardsPlayed()));
                case OWN_REMAINING   : targetedCards.addAll(Arrays.asList(self.getDeck().getCardsInHand()));
                    break;
                case OTHER           : targetedCards.addAll(other.getDeck().getCardsInDeck());
                case OTHER_HAND      : targetedCards.addAll(Arrays.asList(other.getDeck().getCardsPlayed()));
                case OTHER_REMAINING : targetedCards.addAll(Arrays.asList(other.getDeck().getCardsInHand()));
                    break;
                case BOTH            : targetedCards.addAll(self.getDeck().getCardsInDeck());
                    targetedCards.addAll(other.getDeck().getCardsInDeck());
                case BOTH_HAND       : targetedCards.addAll(Arrays.asList(self.getDeck().getCardsPlayed()));
                    targetedCards.addAll(Arrays.asList(other.getDeck().getCardsPlayed()));
                case BOTH_REMAINING  : targetedCards.addAll(Arrays.asList(self.getDeck().getCardsInHand()));
                    targetedCards.addAll(Arrays.asList(other.getDeck().getCardsInHand()));
                    break;
                case COMPLEX         :
                case INIT_FINISHED   :
                case INVALID_STATE   : log.error("Error in Effect Selector initialization!");
            }
            // Remove all null cards that may have com from hand / board
            while (targetedCards.remove(null)){}
        }

        List<Card> tmpCardList = new ArrayList<>();
        for (int i = 1, initStringsLength = initStrings.length; i < initStringsLength; i+=2) {
            TargetQualifiers initQualifiers = TargetQualifiers.fromString(initStrings[i]);

            if (initQualifiers != null && !targetedCards.isEmpty()) {
                switch (initQualifiers) {
                    case BASE_ENERGY_UNDER :
                        int energy = Integer.parseInt(initStrings[i+1]);
                        for (Card card : targetedCards) { if (card.getBaseCost() < energy) { tmpCardList.add(card); }  }
                        targetedCards.clear();
                        targetedCards.addAll(tmpCardList);
                        tmpCardList.clear();
                        break;

                    case BASE_ENERGY_ABOVE :
                        energy = Integer.parseInt(initStrings[i+1]);
                        for (Card card : targetedCards) { if (card.getBaseCost() > energy) { tmpCardList.add(card); }  }
                        targetedCards.clear();
                        targetedCards.addAll(tmpCardList);
                        tmpCardList.clear();
                        break;

                    case FROM_ALBUM:
                        Album album = Album.fromString(initStrings[i+1]);
                        for (Card card : targetedCards) { if (card.getAlbum() == album) { tmpCardList.add(card); }  }
                        targetedCards.clear();
                        targetedCards.addAll(tmpCardList);
                        tmpCardList.clear();
                        break;

                    case FROM_COLLECTION:
                        Collection collection = Collection.fromString(initStrings[i+1]);
                        for (Card card : targetedCards) { if (card.getCollection() == collection) { tmpCardList.add(card); }  }
                        targetedCards.clear();
                        targetedCards.addAll(tmpCardList);
                        tmpCardList.clear();
                        break;

                    case FROM_NAME:
                        String name = initStrings[i+1];
                        for (Card card : targetedCards) { if (card.getName().equals(name)) { tmpCardList.add(card); }  }
                        targetedCards.clear();
                        targetedCards.addAll(tmpCardList);
                        tmpCardList.clear();
                        break;

                    case FROM_NAME_CONTAINS:
                        name = initStrings[i+1];
                        for (Card card : targetedCards) { if (card.getName().contains(name)) { tmpCardList.add(card); }  }
                        targetedCards.clear();
                        targetedCards.addAll(tmpCardList);
                        tmpCardList.clear();
                        break;

                    case INIT_FINISHED     :
                    case INVALID_STATE     :
                    default                : log.error("Error in Effect Selector qualification!");
                }
            }
        }
        return targetedCards;
    }
}

