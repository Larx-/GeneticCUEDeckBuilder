package Controlling;

import Agents.AgentInterface;
import Agents.AgentRandom;
import GameElements.Deck;
import GameElements.Rules;
import Setup.DeckInitializer;
import Setup.RulesInitializer;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.log4j.Log4j2;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class GenAlg {

    public static Rules rules;
    public static final int defaultNumCards = 18;
    public static DeckInitializer deckInitializer;
    public static RulesInitializer rulesInitializer;

    public static final int numResidents = 6;
    public static final int numCandidates = 50;

    public static final int repetitions = 10;
    public static final int tournamentSize = 5;
    public static final int generations = 1000;
    public static final int comboMutationChance = 50;

    public static ResultWriter resultWriter;
    private List<String[]> resDecks;
    private List<String[]> canDecks;

    private final List<AgentInterface> residentList;
    private List<Candidate> candidateList;

    public GenAlg (String cardsFile, String rulesFile, String residentsFile, String candidatesFile) {
        deckInitializer = new DeckInitializer(cardsFile);
        rulesInitializer = new RulesInitializer();

        rules = rulesInitializer.getRulesFromFile(rulesFile);

        resultWriter = new ResultWriter(Main.resultsDir, Main.resultsName);

        this.residentList = this.initResidents(residentsFile);
        this.candidateList = this.initCandidates(candidatesFile);
    }

    public void runGeneticAlgorithm() {
        resultWriter.writeInitial(rules, this.resDecks, this.canDecks);

        FitnessEvaluator evaluator = new FitnessEvaluator(this.residentList);

        for (int gen = 0; gen < generations; gen++) {
            log.debug("Generation " + gen);

            Candidate canBest = evaluator.evaluateFitness(candidateList, gen);
            // Write evaluated candidates, instead of new ones to be able to confirm evaluation, if restarting is necessary, also makes more sense logically
            resultWriter.writeCurrentCandidates(this.canDecks);

            this.candidateList = this.selectNextGen(this.candidateList);
            this.candidateList = this.mutateGen(this.candidateList);

            // Add the best candidate without mutating
            this.candidateList.add(new Candidate(canBest.getDeckStrArray()));
            this.canDecks.add(canBest.getDeckStrArray());
        }
    }

    private List<Candidate> selectNextGen(List<Candidate> candidateList) {
        // Selection (Tournament)
        List<Candidate> newPopulation = new ArrayList<>();
        for (int i = 0; i < numCandidates - 1; i++) {
            Candidate bestParticipant = candidateList.get(Main.random.nextInt(numCandidates));
            for (int participant = 0; participant < tournamentSize-1; participant++) {
                Candidate nextParticipant = candidateList.get(Main.random.nextInt(numCandidates));
                if (nextParticipant.fitness > bestParticipant.fitness) {
                    bestParticipant = nextParticipant;
                }
            }
            newPopulation.add(bestParticipant);
        }
        return newPopulation;
    }

    private List<Candidate> mutateGen(List<Candidate> canList) {
        // Mutation (Single point) TODO: Reducing chance over time and possibly starting with multiple mutations, also crossover
        List<Candidate> mutatedPopulation = new ArrayList<>();
        this.canDecks = new ArrayList<>();

        for (Candidate can : canList) {
            int mutationSpot = Main.random.nextInt(GenAlg.defaultNumCards);
            int comboWithInt = 0;
            String cardStr;

            if (Main.random.nextInt(100) < GenAlg.comboMutationChance) {
                do {
                    comboWithInt = Main.random.nextInt(GenAlg.defaultNumCards);
                } while (comboWithInt == mutationSpot);

                List<String> potentialCombos = deckInitializer.getCardReader().getPotentialCombos(can.getDeckStrArray()[comboWithInt]);

                do {
                    if (potentialCombos.isEmpty()) {
                        cardStr = deckInitializer.getCardReader().getRandomCardStr();
                    } else {
                        int cardInt = Main.random.nextInt(potentialCombos.size());
                        cardStr = potentialCombos.remove(cardInt);
                    }
                } while (can.containsCard(cardStr));

            } else {
                do {
                    cardStr = deckInitializer.getCardReader().getRandomCardStr();
                } while (can.containsCard(cardStr));
            }

            String[] mutatedDeckStr = can.mutate(mutationSpot, cardStr);
            this.canDecks.add(mutatedDeckStr);
            mutatedPopulation.add(new Candidate(mutatedDeckStr));
        }
        return mutatedPopulation;
    }

    private List<AgentInterface> initResidents (String fileName) {
        // Initialization of residents
        List<AgentInterface> resList = new ArrayList<>();

        this.resDecks = this.initDecks(fileName, numResidents);
        for (int i = 0; i < numResidents; i++) {
            Deck deck = deckInitializer.createDeckFromCardList(this.resDecks.get(i));
            resList.add(new AgentRandom(deck));
        }

        return resList;
    }

    private List<Candidate> initCandidates (String fileName) {
        // Initialization of first generation (from file or random)
        List<Candidate> canList = new ArrayList<>();

        this.canDecks = this.initDecks(fileName, numCandidates);
        for (int i = 0; i < numCandidates; i++) {
            canList.add(new Candidate(this.canDecks.get(i)));
        }

        return canList;
    }

    private List<String[]> initDecks (String fileName, int number) {
        List<String[]> decks = new ArrayList<>();
        List<String[]> decksFilled = new ArrayList<>();

        if (fileName != null) {
            try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
                decks = reader.readAll();
            } catch (IOException | CsvException e) {
                log.error(Arrays.toString(e.getStackTrace()));
            }

            // Fill out any missing cards and make sure there are no more than number decks
            for (int j = 0; j < Math.min(number, decks.size()); j++) {
                String[] deck = decks.get(j);

                if (deck.length == GenAlg.defaultNumCards) {
                    decksFilled.add(deck);

                }else if (deck.length > GenAlg.defaultNumCards) {
                    decksFilled.add(Arrays.copyOfRange(deck,0,GenAlg.defaultNumCards));

                } else {
                    List<String> deckFilled = new ArrayList<>();
                    for (int i = 0; i < GenAlg.defaultNumCards; i++) {
                        if (deck.length > i) {
                            deckFilled.add(deck[i]);
                        } else {
                            // Make sure there are no double cards in the deck
                            String cardStr = deckInitializer.getCardReader().getRandomCardStr();
                            while (deckFilled.contains(cardStr)) {
                                cardStr = deckInitializer.getCardReader().getRandomCardStr();
                            }
                            deckFilled.add(cardStr);
                        }
                    }
                    decksFilled.add(deckFilled.toArray(new String[0]));

                }
            }
        }

        // Fill remaining decks with random decks
        for (int i = decks.size(); i < number; i++) {
            Deck deck = deckInitializer.createRandomDeck();
            decksFilled.add(deck.toStringArray());
        }

        return decksFilled;
    }
}
