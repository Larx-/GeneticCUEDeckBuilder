package Controlling;

import Setup.TSVtoCSVPreProcessor;
import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;

@Log4j2
public class Main {

    public static SecureRandom random;
    public static final String cardsFile = "src/main/resources/Cards/cards.csv";
    public static final String rulesFile = "src/main/resources/Rules/rules_1.json";
    public static final String residentsFile = "src/main/resources/Residents/residents.csv";
    public static final String candidatesFile = null;

    public static final String resultsDir = "src/main/resources/Results";
    public static final String resultsName = "Test_1";

    public static final int runMode = 2; // 0 = Only GenAlg, 1 = Only TSVtoCSVPreProcessor, 2 = Both

    public static void main(String[] args) {
        Main.random = new SecureRandom();

        if (runMode >= 1) {
            TSVtoCSVPreProcessor preProcessor = new TSVtoCSVPreProcessor();
            preProcessor.processTSVtoCSV();
        }

        if (runMode != 1) {
            GenAlg genAlg = new GenAlg(cardsFile, rulesFile, residentsFile, candidatesFile);
            genAlg.runGeneticAlgorithm();
        }
    }
}