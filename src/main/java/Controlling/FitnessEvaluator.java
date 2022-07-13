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

    private final List<List<AgentInterface>> residentList;

    public FitnessEvaluator(List<List<AgentInterface>> residentList) {
        this.residentList = residentList;
    }

    // Returns the best candidate, evaluates all others in place
    public Candidate evaluateFitness (List<Candidate> candidateList, int gen) {
        long time = System.currentTimeMillis();

        FitnessCollector collector = new FitnessCollector(GenAlg.numResidents, GenAlg.numCandidates);
        Thread[] threads = new Thread[GenAlg.numThreads];

        // Split workload to Threads
        for (int i = 0; i < GenAlg.numThreads; i++) {
            List<AgentInterface> candidateAgents = new ArrayList<>();
            for (int j = 0; j < GenAlg.numCandidates; j++) {
                candidateAgents.add(new AgentRandom(GenAlg.deckInitializer.createDeckFromCardList(candidateList.get(j).getDeckStrArray())));
            }
            FitnessEvalWorker evaluator = new FitnessEvalWorker(GenAlg.rules, GenAlg.repetitions, collector, this.residentList.get(i), candidateAgents);
            threads[i] = new Thread(evaluator);
            threads[i].start();
        }

        // Wait for threads to finish
        for (int i = 0; i < GenAlg.numThreads; i++) {
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
        int canBestNum = 0;

        for (int i = 0; i < GenAlg.numCandidates; i++) {
            Candidate can = candidateList.get(i);
            float fit = fitness[i];

            can.setFitness(fit);
            fitnessTotal += fit;

            if (canBest == null || fit > canBest.fitness) {
                canBest = can;
                canBestNum = i;
            }
            if (canWorst == null || fit < canWorst.fitness) {
                canWorst = can;
            }
        }
        float avgFitness = fitnessTotal / GenAlg.numCandidates;

        // Debug log
        float calcTime = (float) (System.currentTimeMillis() - time) / 1000;
        log.debug("Best fitness : " + canBest.fitness + " " + Arrays.toString(canBest.getDeckStrArray()));
        log.debug("Avg fitness  : " + avgFitness);
        log.debug("Worst fitness: " + canWorst.fitness + " " + Arrays.toString(canWorst.getDeckStrArray()));
        log.debug("Calculated in: " + calcTime + "s \n");

        GenAlg.resultWriter.appendCurrentFitness(gen, canWorst.getFitness()*100, avgFitness*100,
                canBest.getFitness()*100, collector.getWinPercentDistribution(canBestNum), canBest.getDeckStrArray());

        return canBest;
    }
}
