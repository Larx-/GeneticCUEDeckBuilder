package Controlling;

import Agents.AgentInterface;
import Agents.AgentRandom;
import Enums.Who;
import GameElements.Game;
import GameElements.Rules;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class FitnessEvaluator {

    private final List<AgentInterface> residentList;

    public FitnessEvaluator(List<AgentInterface> residentList) {
        this.residentList = residentList;
    }

    // Returns the best candidate, evaluates all others in place
    public Candidate evaluateFitness (List<Candidate> candidateList, int gen) {
        long time = System.currentTimeMillis();

        FitnessCollector collector = new FitnessCollector(GenAlg.numResidents, GenAlg.numCandidates);

        List<AgentInterface> candidateAgents = new ArrayList<>();
        for (int j = 0; j < GenAlg.numCandidates; j++) {
            candidateAgents.add(new AgentRandom(GenAlg.deckInitializer.createDeckFromCardList(candidateList.get(j).getDeckStrArray())));
        }

        for (int i = 0; i < GenAlg.numResidents; i++) {
            collector.addFitness(this.evalSingleResident(GenAlg.rules, this.residentList.get(i), candidateAgents));
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
        String[] copyBest = canBest.getDeckStrArray().clone();
        String[] copyWorst = canWorst.getDeckStrArray().clone();
        Arrays.sort(copyBest);
        Arrays.sort(copyWorst);
        log.debug("Best fitness : " + canBest.fitness + " " + Arrays.toString(copyBest));
        log.debug("Avg fitness  : " + avgFitness);
        log.debug("Worst fitness: " + canWorst.fitness + " " + Arrays.toString(copyWorst));
        log.debug("Calculated in: " + calcTime + "s \n");

        GenAlg.resultWriter.appendCurrentFitness(gen, canWorst.getFitness()*100, avgFitness*100,
                canBest.getFitness()*100, collector.getWinPercentDistribution(canBestNum), canBest.getDeckStrArray());

        return canBest;
    }

    private Float[] evalSingleResident (Rules rules, AgentInterface res, List<AgentInterface> opponents) {
        Float[] winPercentages = new Float[GenAlg.numCandidates];

        for (int i = 0; i < GenAlg.numCandidates; i++) {
            Game game = new Game(rules, res, opponents.get(i));

            int opponentWins = 0;
            for (int rep = 0; rep < GenAlg.repetitions; rep++) {
                if (game.playGame() == Who.OPPONENT) {
                    opponentWins++;
                }
            }

            winPercentages[i] = (float) opponentWins / GenAlg.repetitions;

        }
        return winPercentages;
    }
}
