package Controlling;

import Agents.AgentInterface;
import Agents.AgentRandom;
import GameElements.Rules;
import Setup.DeckInitializer;
import com.sun.javafx.css.Rule;
import lombok.extern.log4j.Log4j2;
import sun.nio.ch.ThreadPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class FitnessEvaluator {

    // TODO: Make a lot of these static in Main (?)
    private final int numResidents;
    private final int numCandidates;
    private final int numThreads;
    private final int repetitions;
    private final DeckInitializer deckInitializer;
    private final Rules rules;
    private final List<List<AgentInterface>> residentList;

    public FitnessEvaluator(int numResidents, int numCandidates, int numThreads, int repetitions,
                            DeckInitializer deckInitializer, Rules rules, List<List<AgentInterface>> residentList) {
        this.numResidents = numResidents;
        this.numCandidates = numCandidates;
        this.numThreads = numThreads;
        this.repetitions = repetitions;
        this.deckInitializer = deckInitializer;
        this.rules = rules;
        this.residentList = residentList;
    }

    // Returns the best candidate, evaluates all others in place
    public Candidate evaluateFitness (List<Candidate> candidateList) {
        long time = System.currentTimeMillis();

        FitnessCollector collector = new FitnessCollector(this.numResidents, this.numCandidates);
        Thread[] threads = new Thread[this.numThreads];

        // Split workload to Threads
        for (int i = 0; i < this.numThreads; i++) {
            List<AgentInterface> candidateAgents = new ArrayList<>();
            for (int j = 0; j < this.numCandidates; j++) {
                candidateAgents.add(new AgentRandom(this.deckInitializer.createDeckFromCardList(candidateList.get(j).getDeckStrArray())));
            }
            FitnessEvalWorker evaluator = new FitnessEvalWorker(this.rules, this.repetitions, collector, this.residentList.get(i), candidateAgents);
            threads[i] = new Thread(evaluator);
            threads[i].start();
        }

        // Wait for threads to finish
        for (int i = 0; i < this.numThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Calc avg and find best and worst
        float[] fitness = collector.calcAvgWinPercentages();
        float fitnessTotal = 0;
        Candidate canBest = null;
        Candidate canWorst = null;

        for (int i = 0; i < this.numCandidates; i++) {
            Candidate can = candidateList.get(i);
            float fit = fitness[i];

            can.setFitness(fit);
            fitnessTotal += fit;

            if (canBest == null || fit > canBest.fitness) {
                canBest = can;
            }
            if (canWorst == null || fit < canWorst.fitness) {
                canWorst = can;
            }
        }
        float avgFitness = fitnessTotal / this.numCandidates;

        // Debug log
        float calcTime = (float) (System.currentTimeMillis() - time) / 1000;
        log.debug("Best fitness : " + canBest.fitness + " " + Arrays.toString(canBest.getDeckStrArray()));
        log.debug("Avg fitness  : " + avgFitness);
        log.debug("Worst fitness: " + canWorst.fitness + " " + Arrays.toString(canWorst.getDeckStrArray()));
        log.debug("Calculated in: " + calcTime + "s \n");

        return canBest;
    }
}
