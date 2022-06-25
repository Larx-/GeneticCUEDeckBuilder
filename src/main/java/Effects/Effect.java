package Effects;

import Enums.*;
import Enums.TargetCards;
import Enums.TargetQualifiers;
import GameElements.Card;
import GameElements.Deck;
import GameElements.Game;
import GameElements.PlayerManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class Effect {

    @Getter @Setter TriggerTime triggerTime;
    @Getter @Setter List<Target> target;
    @Getter @Setter List<Condition> conditions;

    public Effect(TriggerTime triggerTime, List<Target> target, List<Condition> conditions) {
        this.triggerTime = triggerTime;
        this.target = target;
        this.conditions = conditions;
    }

    public Effect applyEffect (Game game, Who selfPlayer) {
        Target target = this.target.get(0);

        if (target == Target.PLAYER) {
            // 1. Collect Targets
            List<PlayerManager> targetPlayers = this.selectPlayers(game, selfPlayer, target);

            // 2. Check conditions (per card / general)
            // 3. In subclass do effect
            // 4. If required return expiryEffect using inverse

        } else {
            // 1. Collect Targets
            List<Card> targetCards = this.selectCards(game, selfPlayer, target);

            // 2. Check conditions


            // 3. In subclass do effect

            // 4. If required return expiryEffect using inverse

        }

        return null;
    }

    private List<PlayerManager> selectPlayers(Game game, Who selfPlayer, Target toSelect) {
        List<PlayerManager> targetPlayers = new ArrayList<>();

        switch (toSelect.getWho()) {
            case SELF:
                if (selfPlayer == Who.RESIDENT) { targetPlayers.add(game.getResident()); }
                else                            { targetPlayers.add(game.getOpponent()); }
                break;
            case OTHER:
                if (selfPlayer == Who.RESIDENT) { targetPlayers.add(game.getOpponent()); }
                else                            { targetPlayers.add(game.getResident()); }
                break;
            case BOTH:
                targetPlayers.add(game.getResident());
                targetPlayers.add(game.getResident());
                break;
        }

        return targetPlayers;
    }

    private List<Card> selectCards(Game game, Who selfPlayer, Target toSelect) {
        List<Card> potentialCards = new ArrayList<>();
        Who selectWho = toSelect.getWho();
        What selectWhat = toSelect.getWhat();

        // add resident cards
        if (selectWho == Who.BOTH ||
                (selfPlayer == Who.RESIDENT && selectWho == Who.SELF) ||
                (selfPlayer == Who.OPPONENT && selectWho == Who.OTHER)) {
            Deck deck = game.getResident().getDeck();
            potentialCards.addAll(this.selectCardsFromPlace(deck, toSelect));
        }

        // add opponent cards
        if (selectWho == Who.BOTH ||
                (selfPlayer == Who.RESIDENT && selectWho == Who.OTHER) ||
                (selfPlayer == Who.OPPONENT && selectWho == Who.SELF)) {
            Deck deck = game.getOpponent().getDeck();
            potentialCards.addAll(this.selectCardsFromPlace(deck, toSelect));
        }

        // Only include cards fitting What
        if (selectWhat != null) {
            List<Card> targetCards = new ArrayList<>();
            String selectName = toSelect.getName();

            for (Card potentialCard : potentialCards) {
                switch (selectWhat) {
                    case NAME:          if (potentialCard.getName().equals(selectName))           { targetCards.add(potentialCard); } break;
                    case NAME_INCLUDES: if (potentialCard.getName().contains(selectName))         { targetCards.add(potentialCard); } break;
                    case COLLECTION:    if (potentialCard.getCollection().equalsName(selectName)) { targetCards.add(potentialCard); } break;
                    case ALBUM:         if (potentialCard.getAlbum().equalsName(selectName))      { targetCards.add(potentialCard); } break;
                }
            }

            return targetCards;
        } else {
            return potentialCards;
        }
    }

    private List<Card> selectCardsFromPlace(Deck deck, Target toSelect) {
        List<Card> targetCards = new ArrayList<>();
        switch (toSelect) {
            case CARDS_IN_DECK:
                targetCards.addAll(deck.getCardsInDeck());
                targetCards.addAll(Arrays.asList(deck.getCardsInHand()));
                break;
            case CARDS_IN_HAND:
                targetCards.addAll(Arrays.asList(deck.getCardsInHand()));
                targetCards.addAll(Arrays.asList(deck.getCardsPlayed()));
                break;
            case CARDS_IN_REMAINING:
                targetCards.addAll(Arrays.asList(deck.getCardsInHand()));
                break;
        }
        return targetCards;
    }


    protected boolean conditionsFulfilled () {
        if (this.conditions != null) {
            for (Condition condition : this.conditions) {
                if (!condition.checkConditionFulfilled()) {
                    return false;
                }
            }
        }
        return true;
    }
}

