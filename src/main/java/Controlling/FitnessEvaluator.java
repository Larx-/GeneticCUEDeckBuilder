package Controlling;

import Agents.AgentInterface;
import Enums.Who;
import GameElements.Game;
import GameElements.Rules;

import java.util.List;

public class FitnessEvaluator implements Runnable {

    private final Rules rules;
    private final AgentInterface resident;
    private final List<AgentInterface> opponents;
    private final int numCandidates;
    private final int repetitions;

    private Float[] winPercentages;
    private FitnessCollector collector;

    public FitnessEvaluator (Rules rules, int repetitions, FitnessCollector collector, AgentInterface resident, List<AgentInterface> opponents) {
        this.rules = rules;
        this.repetitions = repetitions;
        this.collector = collector;
        this.resident = resident;
        this.opponents = opponents;
        this.numCandidates = this.opponents.size();
        this.winPercentages = new Float[this.numCandidates];
    }

    @Override
    public void run() {
        for (int i = 0; i < this.numCandidates; i++) {
            Game game = new Game(this.rules, this.resident, this.opponents.get(i));

            int opponentWins = 0;
            for (int rep = 0; rep < this.repetitions; rep++) {
                if (game.playGame() == Who.OPPONENT) {
                    opponentWins++;
                }
            }

            this.winPercentages[i] = (float) opponentWins / this.repetitions;
        }
        this.collector.addFitness(this.winPercentages);
    }
}
