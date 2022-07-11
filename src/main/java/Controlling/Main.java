package Controlling;

import Agents.*;
import Enums.Who;
import GameElements.*;
import Setup.*;
import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class Main {

    public static SecureRandom random;

    public static void main(String[] args) {
        Main.random = new SecureRandom();
        DeckInitializer deckInitializer = new DeckInitializer("src/main/resources/Cards/cards.csv");
        RulesInitializer rulesInitializer = new RulesInitializer();
        Rules rules = rulesInitializer.getRulesFromFile("src/main/resources/Rules/rules_1.json");

        int repetitions = 1000;
        int numCandidates = 10;
        int tournamentSize = 5;
        int generations = 1000;

        // Initialization of first generation (from file or random)
        List<Candidate> candidateList = new ArrayList<>();
        for (int i = 0; i < numCandidates; i++) {
            Deck deck = deckInitializer.createRandomDeck();
            AgentInterface agent = new AgentRandom(deck);
            candidateList.add(new Candidate(agent));
//            log.debug(agent.getDeck().toString());
        }

        for (int gen = 0; gen < generations; gen++) {
            log.debug("Generation " + gen);
            // Fitness evaluation TODO: evaluate against preset decks, otherwise avg fitness will never go up
            for (int i = 0; i < numCandidates; i++) {
                for (int j = 0; j < numCandidates; j++) {
                    if (i < j) {
                        Candidate residentCan = candidateList.get(i);
                        Candidate opponentCan = candidateList.get(j);

                        Game game = new Game(rules, residentCan.agent, opponentCan.agent);

                        int residentWins = 0;
                        for (int k = 0; k < repetitions; k++) {
                            if (game.playGame() == Who.RESIDENT) {
                                residentWins++;
                            }
                        }
                        float residentWinPercentage = (float) residentWins / repetitions * 100;
                        float opponentWinPercentage = (float) 100 - residentWins;

                        residentCan.results.add(residentWinPercentage);
                        opponentCan.results.add(opponentWinPercentage);
                    }
                }
            }
            float fitnessTotal = 0;
            for (Candidate can : candidateList) {
                can.fitness = can.addResults() / can.results.size();
                fitnessTotal += can.fitness;
            }
            float avgFitness = fitnessTotal / candidateList.size();
            Candidate canBest = null;
            Candidate canWorst = null;
            for (Candidate can : candidateList) {
                if (canBest == null || can.fitness > canBest.fitness) {
                    canBest = can;
                }
                if (canWorst == null || can.fitness < canWorst.fitness) {
                    canWorst = can;
                }
            }
            log.debug("Best fitness:  " + canBest.fitness + " [" + canBest.agent.getDeck().toString() + "]");
            log.debug("Avg fitness:   " + avgFitness);
            log.debug("Worst fitness: " + canWorst.fitness + " [" + canWorst.agent.getDeck().toString() + "]");

            // Selection (Tournament)
            List<Candidate> newPopulation = new ArrayList<>(numCandidates - 1);
            for (int i = 0; i < numCandidates - 1; i++) {
                Candidate bestParticipant = candidateList.get(random.nextInt(numCandidates));
                for (int participant = 0; participant < tournamentSize; participant++) {
                    Candidate nextParticipant = candidateList.get(random.nextInt(numCandidates));
                    if (nextParticipant.fitness > bestParticipant.fitness) {
                        bestParticipant = nextParticipant;
                    }
                }
                newPopulation.add(bestParticipant);
            }
            candidateList = newPopulation;

            // Mutation (Single point)
            for (Candidate can : candidateList) {
                can.agent.getDeck().returnPlayedCards();
                can.agent.getDeck().returnCardsInHand();
                int mutationSpot = random.nextInt(DeckInitializer.defaultNumCards);
                Card card = deckInitializer.getCardReader().getRandomCard();
                String deckString = can.agent.getDeck().toString();
                String cardId = card.getIdString();
                if (!deckString.contains(cardId)) {
                    can.agent.getDeck().replaceCard(mutationSpot, card);
                }
            }

            // Add the best candidate without mutating
            candidateList.add(canBest);
        }
    }
}