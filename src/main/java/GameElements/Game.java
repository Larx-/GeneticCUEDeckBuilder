package GameElements;

import Agents.AgentInterface;
import Effects.*;
import Setup.Album;
import Setup.Collection;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.locks.Condition;

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

        this.initConditions(this.resident.getDeck().getCardsInDeck(), false);
        this.initConditions(this.opponent.getDeck().getCardsInDeck(), true);
    }

    private void initConditions(List<Card> cardList, boolean isOpponentView) {
        // FIXME: There must be a thousand smarter and better ways to do this
        for (Card card : cardList) {
            EffectCollection cardEff = card.getEffects();
            if (cardEff != null && !cardEff.isFullyInitialized()) {
                for (EffectContainer cardEffCont : cardEff.getEffects()) {
                    // Condition initialization and if possible caching
                    Effect effect = cardEffCont.getEffect();

                    for (ConditionInterface cond : effect.getConditions()) {
                        switch (cond.getClass().getSimpleName()) {
                            case "C_001_AfterRoundX":
                                C_001_AfterRoundX c_001 = (C_001_AfterRoundX)cond;
                                c_001.initialize(this);
                                break;

                            case "C_002_BeforeRoundX":
                                C_002_BeforeRoundX c_002 = (C_002_BeforeRoundX)cond;
                                c_002.initialize(this);
                                break;

                            case "C_010_RoundStatus":
                                C_010_RoundStatus c_010 = (C_010_RoundStatus)cond;
                                c_010.initialize(this, isOpponentView);
                                break;

                            default:
                                log.error("Could not initialize unknown Condition!");
                        }
                    }
                    // Effect initialization as far as possible at least
                    switch (effect.getClass().getSimpleName()) {
                        case "E_001_EPT":
                            E_001_EPT e_001 = (E_001_EPT)effect;
                            if (isOpponentView) {
                                e_001.initialize(this.opponent, this.resident);
                            } else {
                                e_001.initialize(this.resident, this.opponent);
                            }
                            break;

                        case "E_010_PPT":
                            E_010_PPT e_010 = (E_010_PPT)effect;
                            if (isOpponentView) {
                                e_010.initialize(this.opponent, this.resident);
                            } else {
                                e_010.initialize(this.resident, this.opponent);
                            }
                            break;

                        case "E_020_Energy":
                        case "E_030_Power":
                            break;

                        default:
                            log.error("Could not initialize unknown Condition!");
                    }
                }
            }
        }
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
        while (!this.effectStack_ENDofROUND.isEmpty()) {
            this.effectStack_ENDofROUND.remove(0).applyEffect();
        }

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
            this.effectStack_DRAW_START.add(new E_030_Power(bCCards,  roundBonus.getCollectionBonus(), null));
            this.effectStack_DRAW_START.add(new E_030_Power(bACards,  roundBonus.getAlbumBonus(),      null));
            this.effectStack_ENDofROUND.add(new E_030_Power(bCCards, -roundBonus.getCollectionBonus(), null));
            this.effectStack_ENDofROUND.add(new E_030_Power(bACards, -roundBonus.getAlbumBonus(),      null));
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
        List<EffectContainer> effectContainers = new ArrayList<>();
        for (Card card : drawnCardsResident) {
            if (card.getEffects() != null) {
                effectContainers.addAll(card.getEffects().getEffectsWithTrigger(TriggerTime.ON_DRAW));
            }
        }
        for (Card card : drawnCardsOpponent) {
            if (card.getEffects() != null) {
                effectContainers.addAll(card.getEffects().getEffectsWithTrigger(TriggerTime.ON_DRAW));
            }
        }

        // Add START effects to queue
        for (Card card : this.resident.getDeck().getCardsInHand()) {
            if (card.getEffects() != null) {
                effectContainers.addAll(card.getEffects().getEffectsWithTrigger(TriggerTime.ON_START_TURN));
            }
        }
        for (Card card : this.resident.getDeck().getCardsInHand()) {
            if (card.getEffects() != null) {
                effectContainers.addAll(card.getEffects().getEffectsWithTrigger(TriggerTime.ON_START_TURN));
            }
        }
        this.addEffectsToTriggerQueues(effectContainers);

        // execute DRAW / START effects
        while (!this.effectStack_DRAW_START.isEmpty()) {
            Effect effect = this.effectStack_DRAW_START.remove(0);
            effect.applyEffect();
        }

        this.resident.applyUnlockIfSet();         // Fixme: Check if this is correct or does the unlock come before draw effects (relevant for self locking cards)
        this.opponent.applyUnlockIfSet();

        this.logHand();

        this.resident.decideNextTurn();
        this.opponent.decideNextTurn();

        // Add PLAY effects to queue
        effectContainers = new ArrayList<>();
        for (Card card : this.resident.getDeck().getCardsPlayed()) {
            if (card.getEffects() != null) {
                effectContainers.addAll(card.getEffects().getEffectsWithTrigger(TriggerTime.ON_PLAY));
            }
        }
        for (Card card : this.resident.getDeck().getCardsPlayed()) {
            if (card.getEffects() != null) {
                effectContainers.addAll(card.getEffects().getEffectsWithTrigger(TriggerTime.ON_PLAY));
            }
        }
        this.addEffectsToTriggerQueues(effectContainers);

        // execute PLAY effects
        while (!this.effectStack_PLAY.isEmpty()) {
            this.effectStack_PLAY.remove(0).applyEffect();
        }

        int rPow = this.resident.getDeck().calcPower() + this.resident.getPowerPerTurn();
        int oPow = this.opponent.getDeck().calcPower() + this.opponent.getPowerPerTurn();
        this.lastPowerDiff = rPow - oPow;
        this.powerBalance += lastPowerDiff;

        this.logPlay();

        // Add RETURN effects to queue
        effectContainers = new ArrayList<>();
        for (Card card : this.resident.getDeck().getCardsPlayed()) {
            if (card.getEffects() != null) {
                effectContainers.addAll(card.getEffects().getEffectsWithTrigger(TriggerTime.ON_RETURN));
            }
        }
        for (Card card : this.resident.getDeck().getCardsPlayed()) {
            if (card.getEffects() != null) {
                effectContainers.addAll(card.getEffects().getEffectsWithTrigger(TriggerTime.ON_RETURN));
            }
        }
        this.addEffectsToTriggerQueues(effectContainers);

        this.resident.getDeck().returnPlayedCards();
        this.opponent.getDeck().returnPlayedCards();

        // execute RETURN effects
        while (!this.effectStack_RETURN.isEmpty()) {
            this.effectStack_RETURN.remove(0).applyEffect();
        }

        this.resident.updateEnergyAvailable(this.rules.getEnergyMin(), this.rules.getEnergyMax());
        this.opponent.updateEnergyAvailable(this.rules.getEnergyMin(), this.rules.getEnergyMax());

        this.resident.updateDoUnlock(); // Fixme: Check if this is correct or does the unlock come before draw effects (relevant for self locking cards)
        this.opponent.updateDoUnlock();

        this.totalTurnNumber++;

        // execute TIMER based effects
        List<Effect> timedEffects = this.effectStack_TIMED.get(this.totalTurnNumber);
        this.effectStack_TIMED.remove(this.totalTurnNumber);
        while (timedEffects != null && !timedEffects.isEmpty()) {
            timedEffects.remove(0).applyEffect();
        }
    }

    private void initEffectedCards(List<Card> cardsToInit, boolean isOpponentView) {
        for (Card card : cardsToInit) {
            EffectCollection effectColl = card.getEffects();
            if (effectColl != null && !effectColl.isFullyInitialized()) {

                List<EffectContainer> effectCont = effectColl.getEffects();
                if (effectCont != null) {

                    for (EffectContainer effectC : effectCont) {
                        Effect effect = effectC.getEffect();
                        String initString = effect.getInitializationString();

                        if (initString != null) {
                            List<Card> effectedCards = Effect.initializeEffectedCards(initString, this, isOpponentView);
                            effect.setEffectedCards(effectedCards);
                            EffectContainer effectCExp = effectC.getExpiryEffect();
                            if (effectCExp != null) {
                                effectCExp.getEffect().setEffectedCards(effectedCards);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addEffectsToTriggerQueues(List<EffectContainer> effectContainers) {
        for (EffectContainer effectCont : effectContainers) {
            switch (effectCont.getTriggerTime()) {
                case ON_START_GAME : if (this.roundNumber <= 1) { this.effectStack_DRAW_START.add(effectCont.getEffect()); } break;
                case ON_START_ROUND: if (this.turnNumber <= 1)  { this.effectStack_DRAW_START.add(effectCont.getEffect()); } break;
                case ON_START_TURN :
                case ON_DRAW       : this.effectStack_DRAW_START.add(effectCont.getEffect());   break;
                case ON_PLAY       : this.effectStack_PLAY.add(effectCont.getEffect());         break;
                case ON_RETURN     :
                case ON_END_TURN   : this.effectStack_RETURN.add(effectCont.getEffect());       break;
                case ON_END_ROUND  : this.effectStack_ENDofROUND.add(effectCont.getEffect());   break;
                case AFTER_TIME    : int turnToTrigger = this.totalTurnNumber + effectCont.getAfterXTurns();
                                     if (this.effectStack_TIMED.containsKey(turnToTrigger)) {
                                         this.effectStack_TIMED.get(turnToTrigger).add(effectCont.getEffect());
                                     } else {
                                         List<Effect> effectList = new ArrayList<>();
                                         effectList.add(effectCont.getEffect());
                                         this.effectStack_TIMED.put(turnToTrigger, effectList);
                                     }
                default            : log.error("Something went wrong!");
            }
        }
    }

    private void logHand(){
        log.debug("   TURN: " + this.turnNumber);
        this.logHandOrPlay(true);
    }

    private void logPlay(){
        this.logHandOrPlay(false);
    }

    private void logHandOrPlay(boolean logHand) {
        log.debug(String.format("[Resident] Energy: %d (⟳%d) \t PPT: %d", this.resident.getEnergyAvailable(), this.resident.getEnergyPerTurn(), this.resident.getPowerPerTurn()));
        if (logHand) { logCards(this.resident.getDeck().getCardsInHand()); }
        else         { logCards(this.resident.getDeck().getCardsPlayed()); }
        log.debug(String.format("[Opponent] Energy: %d (⟳%d) \t PPT: %d", this.opponent.getEnergyAvailable(), this.opponent.getEnergyPerTurn(), this.opponent.getPowerPerTurn()));
        if (logHand) { logCards(this.opponent.getDeck().getCardsInHand()); }
        else         { logCards(this.opponent.getDeck().getCardsPlayed()); }
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
