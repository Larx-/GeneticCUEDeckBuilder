package Controlling;

import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;

@Log4j2
public class Main {

    public static SecureRandom random;
    public static final String cardsFile = "src/main/resources/Cards/cards.csv";
    public static final String rulesFile = "src/main/resources/Rules/rules_1.json";
    public static final String residentsFile = "src/main/resources/Residents/residents.csv";
    public static final String candidatesFile = null;

    public static void main(String[] args) {
        Main.random = new SecureRandom();
        System.out.println("\n");
        log.warn("Make sure you have run TSVtoCSVPreProcessor.main() if you made changes to the patterns!");
        System.out.println("\n");

        GenAlg genAlg = new GenAlg(cardsFile, rulesFile, residentsFile, candidatesFile);
        genAlg.runGeneticAlgorithm();
    }
}