package Controlling;

import Agents.*;
import Enums.Who;
import GameElements.*;
import Setup.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.log4j.Log4j2;

import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
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
        int numCandidates = 500;
        int tournamentSize = 5;
        int generations = 100;

        // Initialization of residents
        List<AgentInterface> residentList = new ArrayList<>();
        List<String[]> residentDecks = new ArrayList<>();
        int numResidents = 0;
        try (CSVReader reader = new CSVReader(new FileReader("src/main/resources/Cards/residents.csv"))) {
            residentDecks = reader.readAll();
            numResidents = residentDecks.size();
        } catch (IOException | CsvException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
        for (String[] resDeck : residentDecks) {
            String[] resDeckFilled = new String[DeckInitializer.defaultNumCards];
            for (int i = 0; i < DeckInitializer.defaultNumCards; i++) {
                if (resDeck.length > i && !resDeck[i].equals("random")) {
                    resDeckFilled[i] = resDeck[i];
                } else {
                    resDeckFilled[i] = deckInitializer.getCardReader().getRandomCardStr();
                }
            }
            Deck deck = deckInitializer.createDeckFromCardList(resDeckFilled);
            residentList.add(new AgentRandom(deck));
        }

        // Initialization of first generation (from file or random)
        List<Candidate> candidateList = new ArrayList<>();
        for (int i = 0; i < numCandidates; i++) {
            Deck deck = deckInitializer.createRandomDeck();
            candidateList.add(new Candidate(deck));
        }

        for (int gen = 0; gen < generations; gen++) {
            log.debug("Generation " + gen);
            long time = System.currentTimeMillis();

            // Fitness evaluation
            for (int i = 0; i < numResidents; i++) {
                for (int j = 0; j < numCandidates; j++) {
                        Candidate opponentCan = candidateList.get(j);

                        Game game = new Game(rules, residentList.get(i), opponentCan.agent);

                        int opponentWins = 0;
                        for (int k = 0; k < repetitions; k++) {
                            if (game.playGame() == Who.OPPONENT) {
                                opponentWins++;
                            }
                        }
                        float opponentWinPercentage = (float) opponentWins / repetitions * 100;
                        opponentCan.results.add(opponentWinPercentage);
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
            log.debug("Best fitness:  " + canBest.fitness + " " + canBest.agent.getDeck().toString());
            log.debug("Avg fitness:   " + avgFitness);
            log.debug("Worst fitness: " + canWorst.fitness + " " + canWorst.agent.getDeck().toString());
            log.debug("Calculated in: " + (System.currentTimeMillis() - time) + "ms");

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
            candidateList = new ArrayList<>();

            // Mutation (Single point)
            for (Candidate can : newPopulation) {
                int mutationSpot = random.nextInt(DeckInitializer.defaultNumCards);
                String cardStr = deckInitializer.getCardReader().getRandomCardStr();
                String[] mutatedDeckStr = can.mutate(mutationSpot, cardStr);
                Deck mutatedDeck = deckInitializer.createDeckFromCardList(mutatedDeckStr);
                candidateList.add(new Candidate(mutatedDeck));
            }

            // Add the best candidate without mutating
            Deck deck = deckInitializer.createDeckFromCardList(canBest.deckStrArray);
            candidateList.add(new Candidate(deck));
        }
    }
}