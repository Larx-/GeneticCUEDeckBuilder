package Controlling;

import Agents.AgentInterface;
import Agents.AgentPlayer;
import Agents.AgentRandom;
import GameElements.Deck;
import GameElements.Game;
import GameElements.Rules;
import PreProcessing.RegexPreProcessor;
import Setup.CardReader;
import Setup.DeckInitializer;
import Setup.RulesInitializer;
import Setup.TSVtoCSVPreProcessor;
import com.opencsv.CSVWriter;
import lombok.extern.log4j.Log4j2;

import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;

@Log4j2
public class Main {

    public static int numErr = 0;

    public static SecureRandom random;
    public static final String cardsFile = "src/main/resources/Cards/cards.csv";
    public static final String rulesFile = "src/main/resources/Rules/6.json";
    public static final String residentsFile = "src/main/resources/Decks/NewResidents/res_current.csv";
    public static final String allCardsTestFile = "src/main/resources/Decks/allCardsTest.csv";
    public static       String candidatesFile = "src/main/resources/Results/UntilAvg60Run/FROM_r4_c50_WITH_r5_c50/currentCandidates.csv";

    public static final String resultsDir = "src/main/resources/Results/UntilAvg60Run";
    public static final String resultsNameOriginal = "FROM_r45_c50_WITH_r6_c50";

    public static final boolean doPreProcessing = true;
    public static final boolean doPreDefDecks   = false;    // Not in GenAlg
    public static final boolean doEndlessMode   = true;     // Not available for GenAlg mode
    public static final int runMode             = 0;        // 0 = GenAlg, 1 = Player vs Bot, 2 = Bot vs Bot, 3 = Create and run GenAlg with allCardsTest
    public static final int doRepeat            = 0;        // Repeat GenAlg this many times with the best candidates, 0 for off

    // Used for Player vs Bot
    public static final String[] noEffDeck = new String[]{
            "LDG005","LDG006","LDG007","LDG014","LDG009","LDG010",
            "PFF006","OMA006","OMA008","OMA010","OMA011","OMA012",
            "OMA013","OWA001","OWA007","OWA010","LRE009","LMA002",};
    public static final String[] fromStandardRulesDeck = new String[]{  // From counting common occurrences in 1_StandardRules
            "EWW028","LPR012","OCE009","SBB012","SMA025","SOD016",
            "STE019","ACFU001","EES008","EMM034","LVE032","OCP002",
            "OMA013","OWA001","OWA007","OWA010","LRE009","LMA002",};
    public static final String[] fromNoEffDeck = new String[]{          // From counting common occurrences in 4_NoEffects
            "ACFU001","ACRR004","EEE009","EWW032","ORE004","PCA022",
            "SDD028","SFN025","OMA008","OMA010","OMA011","OMA012",
            "OMA013","OWA001","OWA007","OWA010","LRE009","LMA002",};
    public static final String[] residentDeck = noEffDeck;
    public static final String[] opponentDeck = fromNoEffDeck;

    public static void main(String[] args) {
        Main.random = new SecureRandom();

        if (doPreProcessing) {
            runPreProcessor();
        }

        if (runMode == 0) {
            String resultsName = doRepeat > 0 ? resultsNameOriginal + "0" : resultsNameOriginal;
            String residFile = residentsFile;
            runGenAlg(residFile, candidatesFile, resultsName);

            for (int i = 1; i <= doRepeat; i++) {
                resultsName = resultsNameOriginal + i;
                residFile = resultsDir + "/" + resultsNameOriginal + (i-1) + "/currentCandidates.csv";

                runGenAlg(residFile, candidatesFile, resultsName);
            }

        } else if (runMode == 3) {
            createAllCardsCandiates();
            runGenAlg(residentsFile, allCardsTestFile, resultsNameOriginal);

        } else {
            if (doEndlessMode) { loopGames(); }
            else               { runGame();   }
        }
    }

    private static void createAllCardsCandiates() {
        CardReader cardReader = new CardReader(cardsFile);
        Set<String> cards = cardReader.getSetOfAllCards();
        StringBuilder allCardsTestCSV = new StringBuilder();

        int cardNumber = 1;
        for (String card : cards) {
            if (cardNumber >= GenAlg.defaultNumCards) {
                allCardsTestCSV.append(card).append("\n");
                cardNumber = 1;
            } else {
                allCardsTestCSV.append(card).append(",");
                cardNumber++;
            }
        }
        // Remove last ","
        if (cardNumber != 1) {
            allCardsTestCSV.deleteCharAt(allCardsTestCSV.length()-1);
        }

        try {
            FileWriter myWriter = new FileWriter(allCardsTestFile);
            myWriter.write(allCardsTestCSV.toString());
            myWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runPreProcessor() {
        RegexPreProcessor regexPreProcessor = new RegexPreProcessor();
        regexPreProcessor.processCardList();

        TSVtoCSVPreProcessor preProcessor = new TSVtoCSVPreProcessor();
        preProcessor.processTSVtoCSV();
    }

    private static void runGenAlg(String resFile, String canFile, String resultsName) {
        GenAlg genAlg = new GenAlg(cardsFile, rulesFile, resFile, canFile, resultsName);
        genAlg.runGeneticAlgorithm();
    }

    private static void loopGames() {
        Scanner scanner = new Scanner(System.in);
        String inputLine = "y";

        while (inputLine.equalsIgnoreCase("y")) {
            runGame();

            log.debug("Play another game? [Y/n]");
            inputLine = scanner.nextLine();

            if (inputLine.equals("")) {
                inputLine = "y";
            }
        }
    }

    private static void runGame() {
        DeckInitializer deckInitializer = new DeckInitializer(cardsFile);
        RulesInitializer rulesInitializer = new RulesInitializer();
        Rules rules = rulesInitializer.getRulesFromFile(rulesFile);

        Deck d1;
        Deck d2;
        if (doPreDefDecks) {
            d1 = deckInitializer.createRandomDeckWithCardList(residentDeck);
            d2 = deckInitializer.createRandomDeckWithCardList(opponentDeck);
        } else {
            d1 = deckInitializer.createRandomDeck();
            d2 = deckInitializer.createRandomDeck();
        }

        AgentInterface a1 = new AgentRandom(d1);
        AgentInterface a2;
        if (runMode == 1) { a2 = new AgentPlayer(d2); }
        else              { a2 = new AgentRandom(d2); }

        Game game = new Game(rules, a1, a2);
        game.setDoOutput(true);
        game.playGame();
    }
}