package GameElements;

import Agents.AgentInterface;
import Effects.*;
import Enums.TriggerTime;
import Enums.Album;
import Enums.Collection;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;

@Log4j2
public class Game {

    @Getter Rules rules;
    @Getter PlayerManager resident;
    @Getter PlayerManager opponent;

    @Getter int totalTurnNumber;
    @Getter int roundNumber;
    @Getter int turnNumber;
    @Getter int rWin;
    @Getter int oWin;
    @Getter int rPow;
    @Getter int oPow;
    @Getter int powerBalance;
    @Getter int lastPowerDiff;

    // FIFO queues of effects to execute (/revert) at different times
    List<Effect> effectStack_DRAW_START;
    List<Effect> effectStack_PLAY;
    List<Effect> effectStack_RETURN;
    List<Effect> effectStack_ENDofROUND;
    Map<Integer,List<Effect>> effectStack_TIMED;


    public Game(Rules rules, AgentInterface resident, AgentInterface opponent) {
        this.rules = rules;

        this.resident = new PlayerManager(resident, this.rules.getEnergyStarting(), this.rules.getEnergyPerTurn(), 0);
        this.opponent = new PlayerManager(opponent, this.rules.getEnergyStarting(), this.rules.getEnergyPerTurn(), 0);
    }

    public void playGame(){
        this.initializeGame();

        while(this.rWin < 3 && this.oWin <3){
            boolean resWon = playRound();

            if (resWon) { this.rWin++; }
            else        { this.oWin++; }

            log.debug("Resident  "+this.rWin+" - "+this.oWin+"  Opponent");
            this.roundNumber++;
        }

        if (this.rWin > this.oWin) {
            log.debug("--> Resident won game <--");
        } else {
            log.debug("--> Opponent won game <--");
        }
        log.debug("");
        log.debug("");
    }

    private void initializeGame() {
        this.effectStack_DRAW_START = new ArrayList<>();
        this.effectStack_PLAY       = new ArrayList<>();
        this.effectStack_RETURN     = new ArrayList<>();
        this.effectStack_ENDofROUND = new ArrayList<>();
        this.effectStack_TIMED      = new HashMap<>();

        this.rules.chooseRoundBoni();

        this.resident.getDeck().shuffleDeck();
        this.opponent.getDeck().shuffleDeck();

        this.rWin = 0;
        this.oWin = 0;
        this.totalTurnNumber = 1;
        this.roundNumber = 1;

//        this.initConditions(this.resident.getDeck().getCardsInDeck(), false);
//        this.initConditions(this.opponent.getDeck().getCardsInDeck(), true);
    }

    private boolean playRound(){
        this.powerBalance = 0;
        this.turnNumber = 1;

        // Find effected cards and add round bonus to first DRAW and AFTER_ROUND queues
        this.applyRoundBonus();

        // Play round
        for (int i = 0; i < 3; i++){
            this.playTurn();
            this.turnNumber++;
        }

        // Sudden death until ties are resolved
        while (this.powerBalance == 0) {
            this.playTurn();
            this.turnNumber++;
        }

        // execute EndOfRound effects
        this.applyEffectStack("ENDofROUND");

        return this.powerBalance > 0;
    }

    private void applyRoundBonus() {
        RoundBonus roundBonus = this.rules.getRoundBonus(this.roundNumber);

        Collection bonusCollection = roundBonus.getCollection();
        Album bonusAlbum = roundBonus.getAlbum();

        List<Card> bCCards = new ArrayList<>();
        List<Card> bACards = new ArrayList<>();

        // Return by value (I know, not pretty, but more efficient than my previous attempt)
        if (this.opponent.findCardsForBonus(bonusCollection, bonusAlbum, bCCards, bACards)) {
            this.effectStack_DRAW_START.add(new E_Power(bCCards,  roundBonus.getCollectionBonus(), null, TriggerTime.START_ROUND));
            this.effectStack_DRAW_START.add(new E_Power(bACards,  roundBonus.getAlbumBonus(),      null, TriggerTime.START_ROUND));
            this.effectStack_ENDofROUND.add(new E_Power(bCCards, -roundBonus.getCollectionBonus(), null, TriggerTime.END_ROUND));
            this.effectStack_ENDofROUND.add(new E_Power(bACards, -roundBonus.getAlbumBonus(),      null, TriggerTime.END_ROUND));
        }

        log.debug(String.format("ROUND %d  (+%d %s / +%d %s)", this.roundNumber,
                roundBonus.getCollectionBonus(), bonusCollection, roundBonus.getAlbumBonus(), bonusAlbum));
    }

    private void playTurn(){
        List<Card> drawnCardsResident = this.resident.getDeck().drawCards();
        List<Card> drawnCardsOpponent = this.opponent.getDeck().drawCards();

        this.initEffectedCards(Arrays.asList(this.resident.getDeck().getCardsInHand()), false);
        this.initEffectedCards(Arrays.asList(this.opponent.getDeck().getCardsInHand()), true);

        // Add DRAW effects to queue
        for (Card card : drawnCardsResident) {
            if (card.getEffects() != null) {
                this.effectStack_DRAW_START.addAll(card.getEffects().get(TriggerTime.DRAW));
            }
        }
        for (Card card : drawnCardsOpponent) {
            if (card.getEffects() != null) {
                this.effectStack_DRAW_START.addAll(card.getEffects().get(TriggerTime.DRAW));
            }
        }

        // Add START effects to queue
        for (Card card : this.resident.getDeck().getCardsInHand()) {
            if (card.getEffects() != null) {
                this.effectStack_DRAW_START.addAll(card.getEffects().get(TriggerTime.START_TURN));
            }
        }
        for (Card card : this.opponent.getDeck().getCardsInHand()) {
            if (card.getEffects() != null) {
                this.effectStack_DRAW_START.addAll(card.getEffects().get(TriggerTime.START_TURN));
            }
        }

        // execute DRAW / START effects
        this.applyEffectStack("DRAW_START");

        this.resident.applyUnlockIfSet();         // Fixme: Check if this is correct or does the unlock come before draw effects (relevant for self locking cards)
        this.opponent.applyUnlockIfSet();

        this.logHand();

        this.resident.decideNextTurn();
        this.opponent.decideNextTurn();

        // Add PLAY effects to queue
        for (Card card : this.resident.getDeck().getCardsPlayed()) {
            if (card != null && card.getEffects() != null) {
                this.effectStack_PLAY.addAll(card.getEffects().get(TriggerTime.PLAY));
            }
        }
        for (Card card : this.resident.getDeck().getCardsPlayed()) {
            if (card != null && card.getEffects() != null) {
                this.effectStack_PLAY.addAll(card.getEffects().get(TriggerTime.PLAY));
            }
        }

        // execute PLAY effects
        this.applyEffectStack("PLAY");

        // Power calculation
        this.rPow = this.resident.getDeck().calcPower() + this.resident.getPowerPerTurn();
        this.oPow = this.opponent.getDeck().calcPower() + this.opponent.getPowerPerTurn();
        this.lastPowerDiff = rPow - oPow;
        this.powerBalance += this.lastPowerDiff;

        this.logPlay();

        // Add RETURN effects to queue
        for (Card card : this.resident.getDeck().getCardsPlayed()) {
            if (card != null && card.getEffects() != null) {
                this.effectStack_RETURN.addAll(card.getEffects().get(TriggerTime.RETURN));
            }
        }
        for (Card card : this.resident.getDeck().getCardsPlayed()) {
            if (card != null && card.getEffects() != null) {
                this.effectStack_RETURN.addAll(card.getEffects().get(TriggerTime.RETURN));
            }
        }

        this.resident.getDeck().returnPlayedCards();
        this.opponent.getDeck().returnPlayedCards();

        // execute RETURN effects
        this.applyEffectStack("RETURN");

        this.resident.updateEnergyAvailable(this.rules.getEnergyMin(), this.rules.getEnergyMax());
        this.opponent.updateEnergyAvailable(this.rules.getEnergyMin(), this.rules.getEnergyMax());

        this.resident.updateDoUnlock(); // Fixme: Check if this is correct or does the unlock come before draw effects (relevant for self locking cards)
        this.opponent.updateDoUnlock();

        this.totalTurnNumber++;

        // execute TIMER based effects
        this.applyEffectStack("TIMED");
    }

    private void applyEffectStack (String effectStackStr) {
        List<Effect> effectStack = new ArrayList<>();
        switch (effectStackStr) {
            case "DRAW_START"   : effectStack = this.effectStack_DRAW_START;                        break;
            case "PLAY"         : effectStack = this.effectStack_PLAY;                              break;
            case "RETURN"       : effectStack = this.effectStack_RETURN;                            break;
            case "ENDofROUND"   : effectStack = this.effectStack_ENDofROUND;                        break;
            case "TIMED"        : effectStack = this.effectStack_TIMED.remove(this.totalTurnNumber);break;
        }

        // Make sure expiry effects only get added when the effects actually trigger
        List<Effect> expiryEffectCache = new ArrayList<>();

        while (effectStack != null && !effectStack.isEmpty()) {
            Effect expiryEffect = effectStack.remove(0).applyEffect();
            if (expiryEffect != null) {
                expiryEffectCache.add(expiryEffect);
            }
        }
        this.addEffectsToTriggerQueues(expiryEffectCache);
    }

    private void initEffectedCards(List<Card> cardsToInit, boolean isOpponentView) {
        for (Card card : cardsToInit) {
            EffectCollection effectColl = null; //card.getEffects();
            if (effectColl != null && !effectColl.isFullyInitialized()) {

                List<Effect> effectCont = effectColl.getEffects();
                if (effectCont != null) {

                    for (Effect effect : effectCont) {
                        String initString = effect.getInitializationString();

                        if (initString != null) {
                            List<Card> effectedCards = Effect.initializeEffectedCards(initString, this, isOpponentView);
                            effect.setEffectedCards(effectedCards);

                            Effect effectExpiry = effect.getExpiryEffect();
                            if (effectExpiry != null) {
                                effectExpiry.setEffectedCards(effectedCards);
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO: could this be avoided completely with a slightly more clever handling when expiry effects are added?
    private void addEffectsToTriggerQueues(List<Effect> effects) {
        for (Effect effect : effects) {
            switch (effect.getTriggerTime()) {
                case START_GAME: if (this.roundNumber <= 1) { this.effectStack_DRAW_START.add(effect); } break;
                case START_ROUND: if (this.turnNumber <= 1)  { this.effectStack_DRAW_START.add(effect); } break;
                case START_TURN:
                case DRAW: this.effectStack_DRAW_START.add(effect);   break;
                case PLAY: this.effectStack_PLAY.add(effect);         break;
                case RETURN:
                case END_TURN: this.effectStack_RETURN.add(effect);       break;
                case END_ROUND: this.effectStack_ENDofROUND.add(effect);   break;
                case TIMER: int turnToTrigger = this.totalTurnNumber; //+ effect.getAfterXTurns();
                                     if (this.effectStack_TIMED.containsKey(turnToTrigger)) {
                                         this.effectStack_TIMED.get(turnToTrigger).add(effect);
                                     } else {
                                         List<Effect> effectList = new ArrayList<>();
                                         effectList.add(effect);
                                         this.effectStack_TIMED.put(turnToTrigger, effectList);
                                     }
                default            : log.error("Something went wrong!");
            }
        }
    }

    private void logHand(){
        log.debug("   TURN: " + this.turnNumber);
        log.debug(String.format("[Resident] Energy: %d (⟳%d) \t PPT: %d", this.resident.getEnergyAvailable(), this.resident.getEnergyPerTurn(), this.resident.getPowerPerTurn()));
        logCards(this.resident.getDeck().getCardsInHand());

        log.debug(String.format("[Opponent] Energy: %d (⟳%d) \t PPT: %d", this.opponent.getEnergyAvailable(), this.opponent.getEnergyPerTurn(), this.opponent.getPowerPerTurn()));
        logCards(this.opponent.getDeck().getCardsInHand());
        logLeading();
    }

    private void logPlay(){
        log.debug(String.format("[Resident] Energy: %d (⟳%d) \t Power: %d", this.resident.getEnergyAvailable(), this.resident.getEnergyPerTurn(), this.rPow));
        logCards(this.resident.getDeck().getCardsPlayed());

        log.debug(String.format("[Opponent] Energy: %d (⟳%d) \t Power: %d", this.opponent.getEnergyAvailable(), this.opponent.getEnergyPerTurn(), this.oPow));
        logCards(this.opponent.getDeck().getCardsPlayed());
        logLeading();
    }

    private void logCards(Card[] toPrint){
        for (Card card : toPrint) {
            if (card == null) {
                log.debug("  ----------  ");
            } else {
                log.debug("  " + card + "  ");
            }
        }
    }

    private void logLeading() {
        if (this.powerBalance > 0) {
            log.debug("[ Resident is leading round by " + this.powerBalance + " ]");
        } else if (this.powerBalance < 0) {
            log.debug("[ Opponent is leading round by " + (-this.powerBalance) + " ]");
        } else {
            log.debug("[ Round is tied ]");
        }
        log.debug("");
    }
}
