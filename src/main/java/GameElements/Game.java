package GameElements;

import Agents.AgentInterface;
import Effects.*;
import Enums.*;
import Enums.Collection;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;

@Log4j2
public class Game {

    @Getter Rules rules;
    @Getter Player resident;
    @Getter Player opponent;

    @Getter int totalTurnNumber;
    @Getter int roundNumber;
    @Getter int turnNumber;
    @Getter int resWins;
    @Getter int oppWins;
    @Getter int resPower;
    @Getter int oppPower;
    @Getter int powerBalance;
    @Getter int lastPowerDiff;

    @Getter Map<Who,List<Card>> playedCardsHistory;

    // FIFO queues of effects to execute (/revert) at different times
    Map<TriggerTime,List<Effect>> effectStackResident;
    Map<TriggerTime,List<Effect>> effectStackOpponent;
    Map<Integer,List<Effect>> effectStackTimerResident;
    Map<Integer,List<Effect>> effectStackTimerOpponent;

    public Game(Rules rules, AgentInterface resident, AgentInterface opponent) {
        this.rules = rules;

        this.resident = new Player(resident, this.rules.getEnergyStarting(), this.rules.getEnergyPerTurn(), 0);
        this.opponent = new Player(opponent, this.rules.getEnergyStarting(), this.rules.getEnergyPerTurn(), 0);
    }

    public Who playGame(){
        this.initializeGame();

        while(this.resWins < 3 && this.oppWins < 3){
            boolean resWon = playRound();

            if (resWon) { this.resWins++; }
            else        { this.oppWins++; }

//            log.debug("Resident  "+this.rWin+" - "+this.oWin+"  Opponent");
            this.roundNumber++;
        }

        if (this.resWins > this.oppWins) {
//            log.debug("--> Resident won game <--");
//            log.debug("");
//            log.debug("");
            return Who.RESIDENT;

        } else {
//            log.debug("--> Opponent won game <--");
//            log.debug("");
//            log.debug("");
            return Who.OPPONENT;
        }
    }

    private void initializeGame() {
        this.effectStackTimerResident = new HashMap<>();
        this.effectStackTimerOpponent = new HashMap<>();
        this.effectStackResident = new HashMap<>();
        this.effectStackOpponent = new HashMap<>();
        for (TriggerTime triggerTime : TriggerTime.values()) {
            this.effectStackResident.put(triggerTime, new ArrayList<>());
            this.effectStackOpponent.put(triggerTime, new ArrayList<>());
        }

        this.resWins = 0;
        this.oppWins = 0;
        this.totalTurnNumber = 1;
        this.roundNumber = 1;
        this.playedCardsHistory = new HashMap<>();
        this.playedCardsHistory.put(Who.RESIDENT, new ArrayList<>());
        this.playedCardsHistory.put(Who.OPPONENT, new ArrayList<>());

        this.resident.resetPlayer(this.rules.getEnergyStarting(), this.rules.getEnergyPerTurn(), 0);
        this.opponent.resetPlayer(this.rules.getEnergyStarting(), this.rules.getEnergyPerTurn(), 0);

        this.rules.chooseRoundBoni();

        this.resident.getDeck().shuffleDeck();
        this.opponent.getDeck().shuffleDeck();

        this.applyCardEffects(this.resident.getDeck().getCardsInDeck(), TriggerTime.START_GAME, Who.RESIDENT);
        this.applyCardEffects(this.opponent.getDeck().getCardsInDeck(), TriggerTime.START_GAME, Who.OPPONENT);
        this.applyEffectStack(TriggerTime.START_GAME, Who.BOTH);
    }

    private boolean playRound(){
        this.powerBalance = 0;
        this.turnNumber = 1;

        // Find effected cards and add round bonus to first DRAW and AFTER_ROUND queues
        this.applyRoundBonus();
        this.applyEffectStack(TriggerTime.START_ROUND, Who.BOTH);

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
        this.applyEffectStack(TriggerTime.END_ROUND, Who.BOTH);

        return this.powerBalance > 0;
    }

    private void applyRoundBonus() {
        RoundBonus roundBonus = this.rules.getRoundBonus(this.roundNumber);

        Collection bonusCollection = roundBonus.getCollection();
        Album bonusAlbum = roundBonus.getAlbum();

        Target bonusCollectionTarget    = new Target(Who.BOTH,Where.CARDS_IN_DECK,bonusCollection);
        Target bonusAlbumTarget         = new Target(Who.BOTH,Where.CARDS_IN_DECK,bonusAlbum);

        Effect roundBonusCollection     = new E_Power(TriggerTime.START_ROUND, bonusCollectionTarget, roundBonus.getCollectionBonus(),TriggerTime.END_ROUND,null);
        Effect roundBonusAlbum          = new E_Power(TriggerTime.START_ROUND, bonusAlbumTarget, roundBonus.getAlbumBonus(),TriggerTime.END_ROUND,null);

        Effect collectionExpiry         = roundBonusCollection.applyEffect(this, Who.RESIDENT);
        Effect albumExpiry              = roundBonusAlbum.applyEffect(this, Who.RESIDENT);

        this.effectStackResident.get(TriggerTime.END_ROUND).add(collectionExpiry);
        this.effectStackResident.get(TriggerTime.END_ROUND).add(albumExpiry);

        if (roundBonus.getCollection() == null) {
//            log.debug(String.format("ROUND %d  (+%d %s)", this.roundNumber,
//                    roundBonus.getAlbumBonus(), bonusAlbum));
        } else {
//            log.debug(String.format("ROUND %d  (+%d %s / +%d %s)", this.roundNumber,
//                    (roundBonus.getCollectionBonus() + roundBonus.getAlbumBonus()), bonusCollection, roundBonus.getAlbumBonus(), bonusAlbum));
        }

    }

    private void playTurn(){
        // Unlock mechanic, not 100% sure it's in the correct ordering
        this.resident.applyUnlockIfSet();
        this.opponent.applyUnlockIfSet();

        // DRAW effects
        List<Card> drawnCardsResident = this.resident.getDeck().drawCards();
        List<Card> drawnCardsOpponent = this.opponent.getDeck().drawCards();

        this.resident.getDeck().burnCards();
        this.opponent.getDeck().burnCards();

        this.applyCardEffects(drawnCardsResident,TriggerTime.DRAW,Who.RESIDENT);
        this.applyCardEffects(drawnCardsOpponent,TriggerTime.DRAW,Who.OPPONENT);
        this.applyEffectStack(TriggerTime.DRAW,Who.BOTH);

        // START effects
        Card[] cardsInHandResident = this.resident.getDeck().getCardsInHand();
        Card[] cardsInHandOpponent = this.opponent.getDeck().getCardsInHand();

        this.applyCardEffects(cardsInHandResident,TriggerTime.START,Who.RESIDENT);
        this.applyCardEffects(cardsInHandOpponent,TriggerTime.START,Who.OPPONENT);
        this.applyEffectStack(TriggerTime.START,Who.BOTH);

//        this.logHand();

        this.resident.decideNextTurn();
        this.opponent.decideNextTurn();

        // PLAY effects
        Card[] cardsPlayedResident = this.resident.getDeck().getCardsPlayed();
        Card[] cardsPlayedOpponent = this.opponent.getDeck().getCardsPlayed();
        this.savePlayedCards(cardsPlayedResident, cardsPlayedOpponent);

        this.applyCardEffects(cardsPlayedResident,TriggerTime.PLAY,Who.RESIDENT);
        this.applyCardEffects(cardsPlayedOpponent,TriggerTime.PLAY,Who.OPPONENT);
        this.applyEffectStack(TriggerTime.PLAY,Who.BOTH);

        // Power calculation
        this.resPower = this.resident.getDeck().calcPower() + this.resident.getPowerPerTurn();
        this.oppPower = this.opponent.getDeck().calcPower() + this.opponent.getPowerPerTurn();
        this.lastPowerDiff = resPower - oppPower;
        this.powerBalance += this.lastPowerDiff;

//        this.logPlay();

        // RETURN effects
        this.applyCardEffects(cardsPlayedResident,TriggerTime.RETURN,Who.RESIDENT);
        this.applyCardEffects(cardsPlayedOpponent,TriggerTime.RETURN,Who.OPPONENT);
        this.applyCardEffects(cardsPlayedResident,TriggerTime.UNTIL_PLAYED,Who.RESIDENT);
        this.applyCardEffects(cardsPlayedOpponent,TriggerTime.UNTIL_PLAYED,Who.OPPONENT);
        this.applyEffectStack(TriggerTime.RETURN,Who.BOTH);
        this.applyEffectStack(TriggerTime.END_TURN,Who.BOTH);

        this.resident.getDeck().returnPlayedCards();
        this.opponent.getDeck().returnPlayedCards();

        // Update end of turn values
        this.resident.updateEnergyAvailable(this.rules.getEnergyMin(), this.rules.getEnergyMax());
        this.opponent.updateEnergyAvailable(this.rules.getEnergyMin(), this.rules.getEnergyMax());

        // Update if cards should unlock, not 100% sure it's in the correct ordering
        this.resident.updateDoUnlock();
        this.opponent.updateDoUnlock();

        // execute TIMER based effects
        this.applyEffectStack(TriggerTime.TIMER,Who.BOTH);

        this.totalTurnNumber++;
    }

    private void savePlayedCards(Card[] cardsPlayedResident, Card[] cardsPlayedOpponent) {
        for (Card card : cardsPlayedResident) {
            if (card != null) {
                this.playedCardsHistory.get(Who.RESIDENT).add(card);
            }
        }
        for (Card card : cardsPlayedOpponent) {
            if (card != null) {
                this.playedCardsHistory.get(Who.OPPONENT).add(card);
            }
        }
    }

    private void applyCardEffects (Card[] cards, TriggerTime triggerTime, Who selfPlayer) {
        List<Card> cardList = new ArrayList<>();
        for (Card card : cards) {
            if (card != null) {
                cardList.add(card);
            }
        }
        this.applyCardEffects(cardList, triggerTime, selfPlayer);
    }

    private void applyCardEffects (List<Card> cards, TriggerTime triggerTime, Who selfPlayer) {
        for (Card card : cards) {
            if (triggerTime == TriggerTime.UNTIL_PLAYED) {
                card.applyExpiryEffects(this, selfPlayer);

            } else {
                List<Effect> cardEffects = card.getEffectsByTriggerTime(triggerTime);
                if (cardEffects != null) {
                    for (Effect effect : cardEffects) {
                        this.applyEffect(effect, selfPlayer);
                    }
                }
            }
        }
    }

    private void applyEffect (Effect effect, Who selfPlayer) {
        if (effect == null) { return; } // TODO: Figure out why there even is a null effect being added

        Effect expiryEffect = effect.applyEffect(this, selfPlayer);

        if (expiryEffect != null) {
            TriggerTime triggerTime = expiryEffect.getTriggerTime();

            if (triggerTime == TriggerTime.TIMER) {
                int timer = this.totalTurnNumber + expiryEffect.getTimer();

                if (selfPlayer == Who.RESIDENT) {
                    if (!this.effectStackTimerResident.containsKey(timer)) {
                        this.effectStackTimerResident.put(timer, new ArrayList<>());
                    }
                    this.effectStackTimerResident.get(timer).add(expiryEffect);

                } else {
                    if (!this.effectStackTimerOpponent.containsKey(timer)) {
                        this.effectStackTimerOpponent.put(timer, new ArrayList<>());
                    }
                    this.effectStackTimerOpponent.get(this.totalTurnNumber + expiryEffect.getTimer()).add(expiryEffect);
                }

            } else {
                if (selfPlayer == Who.RESIDENT) {
                    this.effectStackResident.get(triggerTime).add(expiryEffect);
                } else {
                    this.effectStackOpponent.get(triggerTime).add(expiryEffect);
                }
            }
        }
    }

    private void applyEffectStack (TriggerTime triggerTime, Who selfPlayer) {
        if (selfPlayer == Who.BOTH) {
            this.applyEffectStack(triggerTime, Who.RESIDENT);
            this.applyEffectStack(triggerTime, Who.OPPONENT);

        } else {
            List<Effect> effectList = null;

            if (triggerTime == TriggerTime.TIMER) {
                if (selfPlayer == Who.RESIDENT) {
                    if (this.effectStackTimerResident.containsKey(this.totalTurnNumber)) {
                        effectList = this.effectStackTimerResident.get(this.totalTurnNumber);
                    }
                } else {
                    if (this.effectStackTimerResident.containsKey(this.totalTurnNumber)) {
                        effectList = this.effectStackTimerOpponent.get(this.totalTurnNumber);
                    }
                }

            } else {
                if (selfPlayer == Who.RESIDENT) {
                    effectList = this.effectStackResident.get(triggerTime);
                } else {
                    effectList = this.effectStackOpponent.get(triggerTime);
                }
            }

            if (effectList != null) {
                while (effectList.size() > 0) {
                    Effect effect = effectList.remove(0);
                    this.applyEffect(effect, selfPlayer);
                }
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
        log.debug(String.format("[Resident] Energy: %d (⟳%d) \t Power: %d", this.resident.getEnergyAvailable(), this.resident.getEnergyPerTurn(), this.resPower));
        logCards(this.resident.getDeck().getCardsPlayed());

        log.debug(String.format("[Opponent] Energy: %d (⟳%d) \t Power: %d", this.opponent.getEnergyAvailable(), this.opponent.getEnergyPerTurn(), this.oppPower));
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
