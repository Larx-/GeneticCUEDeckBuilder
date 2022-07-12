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

    public boolean allFitnessCollected () {
        return this.numResidents == winPercentages.size();
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

    public void resetWinPercentages () {
        this.winPercentages = new ArrayList<>();
    }
}
