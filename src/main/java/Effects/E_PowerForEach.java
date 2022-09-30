package Effects;

import EffectConditions.Condition;
import Enums.TriggerTime;
import Enums.Who;
import GameElements.Card;
import GameElements.Game;
import GameElements.Target;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class E_PowerForEach extends Effect {

    @Getter public Integer changeBy;
    int upTo;
    boolean countPlayHistory;
    @Getter public Target countEach;

    public E_PowerForEach(TriggerTime triggerTime, Target targetCards, Integer changeBy, TriggerTime duration, Integer timer, List<Condition> conditions, Target countEach, int upTo, boolean countPlayHistory) {
        super(triggerTime, targetCards, duration, timer, conditions);

        this.changeBy = changeBy;
        this.countEach = countEach;
        this.upTo = upTo;
        this.countPlayHistory = countPlayHistory;
    }

    public E_PowerForEach(TriggerTime triggerTime, Target targetCards, Integer changeBy, TriggerTime duration, List<Condition> conditions, Target countEach, int upTo, boolean countPlayHistory) {
        super(triggerTime, targetCards, duration, conditions);

        this.changeBy = changeBy;
        this.countEach = countEach;
        this.upTo = upTo;
        this.countPlayHistory = countPlayHistory;
    }

    @Override
    public Effect applyEffect(Game game, Who selfPlayer) {
        // 0. Are cards meant?
        if (super.target.getWhere() != null || this.target.hasPresetCards()) {
            // 1. Collect Targets
            List<Card> targetCards = super.selectCards(game, selfPlayer);

            // 2. Check conditions (per card / general)
            if (!targetCards.isEmpty() && super.conditionsFulfilled(game, selfPlayer)) {
                // 3. In subclass do effect
                int fittingCardsNum = 0;

                if (this.countPlayHistory) {
                    List<Card> cardList = new ArrayList<>();
                    switch (this.countEach.getWho()) { // TODO: write central converter for this
                        case BOTH:
                            cardList.addAll(game.getPlayedCardsHistory().get(Who.RESIDENT));
                            cardList.addAll(game.getPlayedCardsHistory().get(Who.OPPONENT));
                            break;

                        case SELF:
                            if (selfPlayer == Who.RESIDENT) {
                                cardList.addAll(game.getPlayedCardsHistory().get(Who.RESIDENT));
                            } else {
                                cardList.addAll(game.getPlayedCardsHistory().get(Who.OPPONENT));
                            }
                            break;

                        case OTHER:
                            if (selfPlayer == Who.RESIDENT) {
                                cardList.addAll(game.getPlayedCardsHistory().get(Who.OPPONENT));
                            } else {
                                cardList.addAll(game.getPlayedCardsHistory().get(Who.RESIDENT));
                            }
                        default:
                            log.error("Error in condition!");
                    }

                    for (Card c : cardList) { // TODO: Also this should be central, so it's not doubled with C_DeckContains
                        switch (this.countEach.getWhat()) {
                            case NAME:          if (c.getName().equalsIgnoreCase(this.countEach.getName()))       { fittingCardsNum++; } break;
                            case NAME_CONTAINS: if (c.getName().contains(this.countEach.getName().toLowerCase())) { fittingCardsNum++; } break;
                            case ALBUM:         if (c.getAlbum().equalsName(this.countEach.getName()))            { fittingCardsNum++; } break;
                            case COLLECTION:    if (c.getCollection().equalsName(this.countEach.getName()))       { fittingCardsNum++; } break;
                            default:            log.error("Error in condition!");
                        }
                    }

                } else {
                    List<Card> countEachCards = super.selectCards(this.countEach, game, selfPlayer);
                    fittingCardsNum = countEachCards.size();
                }
                int change = this.changeBy * Math.min(fittingCardsNum,this.upTo);

                for (Card card : targetCards) {
                    int newPower = card.getModifierPower() + change;
                    card.setModifierPower(newPower);
                }

                // 4. If required return expiryEffect using inverse
                if (super.duration != null && super.duration != TriggerTime.PERMANENT) {
                    if (super.duration == TriggerTime.UNTIL_PLAYED) {
                        for (Card card : targetCards) {
                            card.getExpiryEffectsAfterPlayed().add(new E_Power(super.duration, null, (-change), null, null));
                        }
                    } else {
                        Target selectedTargetCards = new Target(targetCards);
                        return new E_Power(super.duration, selectedTargetCards, (-change), null, super.timer, null);
                    }
                }
            }
        } else {
            log.error("POWER should not be directly applied to players! Use POWER_PER_TURN instead!");
        }
        return null;
    }
}
