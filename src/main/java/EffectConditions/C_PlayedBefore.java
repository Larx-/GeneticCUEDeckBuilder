package EffectConditions;

import Enums.What;
import Enums.Who;
import GameElements.Card;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class C_PlayedBefore extends Condition {

    Who who;
    What what;
    String compareTo;

    public C_PlayedBefore(Who who, What what, String compareTo) {
        this.who = who;
        this.what = what;
        this.compareTo = compareTo;
    }

    @Override
    public boolean checkConditionFulfilled (Game game, Who selfPlayer) {
        List<Card> cardList = new ArrayList<>();
        switch (this.who) {
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

        for (Card c : cardList) {
            switch (this.what) {
                case NAME:
                    if (c.getName().equalsIgnoreCase(this.compareTo)) { return true; } break;
                case ALBUM:
                    if (c.getAlbum().equalsName(this.compareTo))      { return true; } break;
                case COLLECTION:
                    if (c.getCollection().equalsName(this.compareTo)) { return true; } break;
                default:
                    log.error("Error in condition!");
            }
        }
        return false;
    }
}
