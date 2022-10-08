package Controlling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DeckDistances {

    // Quick and not very pretty, but should get the task done
    public static String fileName_1 = "src/main/resources/Decks/NewResidents/res_identical.csv";
    public static String fileName_2 = null; // "src/main/resources/Decks/NewResidents/res_identical.csv";

    public static void main(String[] args) {

        Integer[][] scoreMatrix = null;

        if (fileName_2 == null) {
            // Internal population homogeneity
            List<String[]> population = readFullFile(fileName_1);
            int populationSize = population.size();

            scoreMatrix = new Integer[populationSize][populationSize];
            int countChecked = 0;
            int scoreTotal = 0;

            for (int i = 0; i < populationSize; i++) {
                for (int j = 1; j < populationSize; j++) {
                    if (j > i) {
                        scoreMatrix[i][j] = scoreSimilarity(population.get(i), population.get(j));
                        scoreTotal += scoreMatrix[i][j];
                        countChecked++;
                    }
                }
            }
            float similarity = scoreTotal / ((float) (countChecked * GenAlg.defaultNumCards) / 100);
            System.out.printf("Similarity: %1.2f%% (Score: %d/%d) \n", similarity, scoreTotal, (countChecked * GenAlg.defaultNumCards));

        } else {
            // Comparing population homogeneity
            List<String[]> population_1 = readFullFile(fileName_1);
            List<String[]> population_2 = readFullFile(fileName_2);
            int populationSize_1 = population_1.size();
            int populationSize_2 = population_2.size();

            scoreMatrix = new Integer[populationSize_1][populationSize_2];
            int countChecked = 0;
            int scoreTotal = 0;

            for (int i = 0; i < populationSize_1; i++) {
                for (int j = 0; j < populationSize_2; j++) {
                    scoreMatrix[i][j] = scoreSimilarity(population_1.get(i), population_2.get(j));
                    scoreTotal += scoreMatrix[i][j];
                    countChecked++;
                }
            }
            float similarity = scoreTotal / ((float) (countChecked * GenAlg.defaultNumCards) / 100);
            System.out.printf("Similarity: %1.2f%% (Score: %d/%d) \n", similarity, scoreTotal, (countChecked * GenAlg.defaultNumCards));
        }

        for (Integer[] row : scoreMatrix) {
            for (Integer cell : row) {
                if (cell != null) {
                    System.out.print(cell);
                }
                System.out.print("\t");
            }
            System.out.print("\n");
        }
    }

    private static int scoreSimilarity(String[] can_1, String[] can_2) {
        List<String> can_2_list = Arrays.asList(can_2);
        int score = 0;

        for (String can_gene : can_1) {
            if (can_2_list.contains(can_gene)) {
                score++;
            }
        }
        return score;
    }

    public static List<String[]> readFullFile (String fileName) {
        List<String[]> cardArray = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                cardArray.add(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cardArray;
    }
}
