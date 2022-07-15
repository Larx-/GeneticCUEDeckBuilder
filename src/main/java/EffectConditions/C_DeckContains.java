package EffectConditions;

import Enums.What;
import Enums.Who;
import GameElements.Card;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class C_DeckContains extends Condition {

    Who who;
    What what;
    String compareTo;
    String sign;
    int number;

    public C_DeckContains(Who who, What what, String compareTo, String signedRounds) {
        this.who = who;
        this.what = what;
        this.compareTo = compareTo;

        this.sign = "==";

        if (signedRounds.charAt(0) == '>' || signedRounds.charAt(0) == '<') {
            if (signedRounds.charAt(1) == '=') {
                this.sign = signedRounds.substring(0,2);
                signedRounds = signedRounds.substring(2);

            } else {
                this.sign = signedRounds.substring(0,1);
                signedRounds = signedRounds.substring(1);
            }
        }

        this.number = Integer.parseInt(signedRounds);
    }

    @Override
    public boolean checkConditionFulfilled(Game game, Who selfPlayer) {
        int deckContains = 0;

        List<Card> cardList = new ArrayList<>();
        switch (this.who) {
            case BOTH:
                cardList.addAll(game.getResident().getCardsInDeck());
                cardList.addAll(game.getOpponent().getCardsInDeck());
                break;

            case SELF:
                if (selfPlayer == Who.RESIDENT) { cardList.addAll(game.getResident().getCardsInDeck());
                } else {                          cardList.addAll(game.getOpponent().getCardsInDeck()); }
                break;

            case OTHER:
                if (selfPlayer == Who.RESIDENT) { cardList.addAll(game.getOpponent().getCardsInDeck());
                } else {                          cardList.addAll(game.getResident().getCardsInDeck()); }

            default:
                log.error("Error in condition!");
        }

        for (Card c : cardList) {
            switch (this.what) {
                case NAME:          if (c.getName().equalsIgnoreCase(this.compareTo))       { deckContains++; } break;
                case NAME_CONTAINS: if (c.getName().contains(this.compareTo.toLowerCase())) { deckContains++; } break;
                case ALBUM:         if (c.getAlbum().equalsName(this.compareTo))            { deckContains++; } break;
                case COLLECTION:    if (c.getCollection().equalsName(this.compareTo))       { deckContains++; } break;
                default:            log.error("Error in condition!");
            }
        }

        switch (this.sign) {
            case ">":  return deckContains >  this.number;
            case ">=": return deckContains >= this.number;
            case "<=": return deckContains <= this.number;
            case "<":  return deckContains <  this.number;
            case "==":
            default:   return deckContains == this.number;
        }
    }
}