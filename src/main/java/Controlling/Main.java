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
        int numCandidates = 50;
        int tournamentSize = 5;
        int generations = 1000;

        int numThreads = 3;

        // Initialization of residents
        List<List<AgentInterface>> residentList = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            residentList.add(new ArrayList<>());
        }
        List<String[]> residentDecks = new ArrayList<>();
        int numResidents = 0;
        try (CSVReader reader = new CSVReader(new FileReader("src/main/resources/Cards/residents.csv"))) {
            residentDecks = reader.readAll();
            numResidents = residentDecks.size();
        } catch (IOException | CsvException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

        for (int j = 0; j < numResidents; j++) {
            String[] resDeck = residentDecks.get(j);
            String[] resDeckFilled = new String[DeckInitializer.defaultNumCards];
            for (int i = 0; i < DeckInitializer.defaultNumCards; i++) {
                if (resDeck.length > i && !resDeck[i].equals("random")) {
                    resDeckFilled[i] = resDeck[i];
                } else {
                    resDeckFilled[i] = deckInitializer.getCardReader().getRandomCardStr();
                }
            }
            Deck deck = deckInitializer.createDeckFromCardList(resDeckFilled);

            residentList.get(j % numThreads).add(new AgentRandom(deck));
        }

        // Initialization of first generation (from file or random)
        List<Candidate> candidateList = new ArrayList<>();
        for (int i = 0; i < numCandidates; i++) {
            Deck deck = deckInitializer.createRandomDeck();
            candidateList.add(new Candidate(deck.toStringArray()));
        }

        FitnessEvaluator evaluator = new FitnessEvaluator(numResidents, numCandidates, numThreads, repetitions, deckInitializer, rules, residentList);

        for (int gen = 0; gen < generations; gen++) {
            log.debug("Generation " + gen);

            // Evaluate candidates
            Candidate canBest = evaluator.evaluateFitness(candidateList);

            // Selection (Tournament)
            List<Candidate> newPopulation = new ArrayList<>(numCandidates - 1);
            for (int i = 0; i < numCandidates - 1; i++) {
                Candidate bestParticipant = candidateList.get(random.nextInt(numCandidates));
                for (int participant = 0; participant < tournamentSize-1; participant++) {
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
                candidateList.add(new Candidate(mutatedDeckStr));
            }

            // Add the best candidate without mutating
            candidateList.add(new Candidate(canBest.deckStrArray));
        }
    }
}