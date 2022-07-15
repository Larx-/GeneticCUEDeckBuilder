package Controlling;

import GameElements.Rules;
import lombok.Getter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ResultWriter {

    public static final boolean SAFE_MODE = false; // Makes sure to never override old files, even when name is the same

    private String name;
    private String directory;
    private Path filePath;

    public ResultWriter (String directory, String name) {
        this.name = name;
        this.directory = directory;

        this.filePath = Paths.get(directory,name);

        if (SAFE_MODE) {
            while (Files.exists(this.filePath)) {
                this.filePath = Paths.get(directory,name,"_new");
            }
        }
    }

    public void writeInitial (Rules rules, List<String[]> residentDecks, List<String[]> initialCandidateDecks) {
        Path initialFilePath = Paths.get(this.filePath.toString(), "initialSettings.txt");

        if (!SAFE_MODE || !Files.exists(initialFilePath)) {
            StringBuilder initalInfo = new StringBuilder("Settings for genetic algorithm:\n");
            initalInfo.append("   Number of residents:       ").append(GenAlg.numResidents).append("\n");
            initalInfo.append("   Number of candidates:      ").append(GenAlg.numCandidates).append("\n");
            initalInfo.append("   Number of repetitions:     ").append(GenAlg.repetitions).append("\n");
            initalInfo.append("   Selection tournament size: ").append(GenAlg.tournamentSize).append("\n");
            initalInfo.append("   Max number of generations: ").append(GenAlg.generations).append("\n");
            initalInfo.append("   Chance for combo mutation: ").append(GenAlg.comboMutationChance).append("\n");

            initalInfo.append("\n").append(rules.toString()).append("\n\n");

            initalInfo.append("Resident decks:\n");
            for (String[] deck : residentDecks) {
                String[] copyDeck = deck.clone();
                Arrays.sort(copyDeck);
                initalInfo.append("   ").append(Arrays.toString(copyDeck)).append("\n");
            }
            initalInfo.append("\n");

            initalInfo.append("Initial candidate decks:\n");
            for (String[] deck : initialCandidateDecks) {
                String[] copyDeck = deck.clone();
                Arrays.sort(copyDeck);
                initalInfo.append("   ").append(Arrays.toString(copyDeck)).append("\n");
            }

            try {
                File file = initialFilePath.toFile();
                file.getParentFile().mkdirs();

                FileWriter myWriter = new FileWriter(file);
                myWriter.write(initalInfo.toString());
                myWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeCurrentCandidates (List<String[]> currentCandidateDecks) {
        Path initialFilePath = Paths.get(this.filePath.toString(), "currentCandidates.txt");

            StringBuilder currentCandidates = new StringBuilder();
            for (String[] deck : currentCandidateDecks) {
                String[] copyDeck = deck.clone();
                Arrays.sort(copyDeck);
                currentCandidates.append(Arrays.toString(copyDeck).replace("[","").replace("]","")).append("\n");
            }

            try {
                FileWriter myWriter = new FileWriter(initialFilePath.toFile());
                myWriter.write(currentCandidates.toString());
                myWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void appendCurrentFitness (int gen, float worst, float avg, float best, float[] bestDistribution, String[] bestDeck) {
        Path initialFilePath = Paths.get(this.filePath.toString(), "continuousFitness.csv");

        if (!Files.exists(initialFilePath) || (!SAFE_MODE && gen == 0)) {
            String header = "Generation, WorstFit, AvgFit, BestFit, BestFirDistribution, BestDeck\n";

            try {
                FileWriter myWriter = new FileWriter(initialFilePath.toFile());
                myWriter.write(header);
                myWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String[] copyBest = bestDeck.clone();
        Arrays.sort(copyBest);
        String currentFitness = gen + ", " + worst + ", " + avg + ", " + best + ", \"" + Arrays.toString(bestDistribution) + "\", \"" + Arrays.toString(copyBest) + "\"\n";

        try {
            FileWriter myWriter = new FileWriter(initialFilePath.toFile(), true);
            myWriter.write(currentFitness);
            myWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
