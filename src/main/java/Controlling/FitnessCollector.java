package Controlling;

import java.util.ArrayList;
import java.util.List;

public class FitnessCollector {

    List<Float[]> winPercentages;
    int numResidents;
    int numOpponents;

    public FitnessCollector (int numResidents, int numOpponents) {
        this.winPercentages = new ArrayList<>();
        this.numResidents = numResidents;
        this.numOpponents = numOpponents;
    }

    public synchronized void addFitness (Float[] opponentWinPercentages) {
        this.winPercentages.add(opponentWinPercentages);
    }

    public float[] calcAvgWinPercentages () {
        float[] avgWinPercent = new float[this.numOpponents];

        for (Float[] percent : this.winPercentages) {
            for (int i = 0; i < this.numOpponents; i++) {
                avgWinPercent[i] += percent[i];
            }
        }

        for (int i = 0; i < this.numOpponents; i++) {
            avgWinPercent[i] = avgWinPercent[i] / this.numResidents;
        }

        return avgWinPercent;
    }

    public float[] getWinPercentDistribution (int candidateNum) {
        float[] winDistribution = new float[this.numResidents];

        for (int i = 0; i < this.numResidents; i++) {
            winDistribution[i] = this.winPercentages.get(i)[candidateNum];
        }

        return winDistribution;
    }

    public void resetWinPercentages () {
        this.winPercentages = new ArrayList<>();
    }
}
