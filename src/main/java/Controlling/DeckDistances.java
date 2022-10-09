package Controlling;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DeckDistances {

    // Quick and not very pretty, but should get the task done
    public static String fileBase   = "src/main/resources/";
    public static String fileOutput = fileBase + "Results/4_DeckDistances_ofAvg60Run/";  // null for no output

    public static String fileName_1 = "Results/3_Min50Gens_UntilAvgScore60/FROM_null_WITH_r6_c95";
    public static String fileName_2 = "Results/3_Min50Gens_UntilAvgScore60/FROM_r45_c0_WITH_r6_c0";

    // Missing: Continuous and Combo 50/95

    public static void main(String[] args) {
        String toFileContent = "Deck Distance Score for: \n      " + fileName_1;
        toFileContent = fileName_2 != null ? toFileContent + " (Left side from top to bottom)\nand   " + fileName_2 + " (Top side from left to right)" : toFileContent;
        toFileContent += "\n\n";

        Integer[][] scoreMatrix = null;

        if (fileName_2 == null) {
            // Internal population homogeneity
            String fileName_str = fileName_1.endsWith(".csv") ? fileBase + fileName_1 : fileBase +  fileName_1 + "/currentCandidates.csv";
            List<String[]> population = readFullFile(fileName_str);
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
            toFileContent += String.format("Similarity: %1.2f%% (Score: %d/%d) \n\n", similarity, scoreTotal, (countChecked * GenAlg.defaultNumCards));

        } else {
            // Comparing population homogeneity
            String fileName_1_str = fileName_1.endsWith(".csv") ? fileBase + fileName_1 : fileBase +  fileName_1 + "/currentCandidates.csv";
            String fileName_2_str = fileName_2.endsWith(".csv") ? fileBase +  fileName_2 : fileBase +  fileName_2 + "/currentCandidates.csv";
            List<String[]> population_1 = readFullFile(fileName_1_str);
            List<String[]> population_2 = readFullFile(fileName_2_str);
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
            toFileContent += String.format("Similarity: %1.2f%% (Score: %d/%d) \n\n", similarity, scoreTotal, (countChecked * GenAlg.defaultNumCards));
        }

        for (Integer[] row : scoreMatrix) {
            for (Integer cell : row) {
                if (cell != null) {
                    toFileContent += cell;
                }
                toFileContent += "\t";
            }
            toFileContent = toFileContent.trim() + "\n";
        }

        if (fileOutput != null) {
            String outputFilePath = fileOutput + fileName_1.replace("/","-");
            outputFilePath = fileName_2 != null ? outputFilePath + "_AND_" + fileName_2.replace("/","-") : outputFilePath;

            System.out.println(outputFilePath);
            System.out.println(toFileContent);

            writeFile(outputFilePath, toFileContent);
        }
    }

    private static void writeFile (String outputFilePath, String content) {
        try {
            Path filePath = Paths.get(outputFilePath);
            File file = filePath.toFile();
            file.getParentFile().mkdirs();

            FileWriter myWriter = new FileWriter(file);
            myWriter.write(content);
            myWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
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
