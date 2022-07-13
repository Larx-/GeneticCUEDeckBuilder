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
            StringBuilder initalInfo = new StringBuilder(rules.toString() + "\n\n");

            initalInfo.append("Resident decks:\n");
            for (String[] deck : residentDecks) {
                Arrays.sort(deck);
                initalInfo.append("   ").append(Arrays.toString(deck)).append("\n");
            }
            initalInfo.append("\n");

            initalInfo.append("Initial candidate decks:\n");
            for (String[] deck : initialCandidateDecks) {
                Arrays.sort(deck);
                initalInfo.append("   ").append(Arrays.toString(deck)).append("\n");
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
                Arrays.sort(deck);
                currentCandidates.append(Arrays.toString(deck)).append("\n");
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

        if (!Files.exists(initialFilePath)) {
            String header = "Generation, WorstFit, AvgFit, BestFit, BestFirDistribution, BestDeck\n";

            try {
                FileWriter myWriter = new FileWriter(initialFilePath.toFile());
                myWriter.write(header);
                myWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Arrays.sort(bestDeck);
        String currentFitness = gen + ", " + worst + ", " + avg + ", " + best + ", \"" + Arrays.toString(bestDistribution) + "\", \"" + Arrays.toString(bestDeck) + "\"\n";

        try {
            FileWriter myWriter = new FileWriter(initialFilePath.toFile(), true);
            myWriter.write(currentFitness);
            myWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
