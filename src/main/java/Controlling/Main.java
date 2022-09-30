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
    public static final String rulesFile = "src/main/resources/Rules/Default.json";
    public static final String residentsFile = "src/main/resources/Decks/residents.csv";
    public static final String allCardsTestFile = "src/main/resources/Decks/allCardsTest.csv";
    public static       String candidatesFile = null;

    public static final String resultsDir = "src/main/resources/Results";
    public static final String resultsName = "Test_2";

    public static final boolean doPreProcessing = true;
    public static final boolean doPreDefDecks   = false;    // Not in GenAlg
    public static final boolean doEndlessMode   = true;     // Not available for GenAlg mode
    public static final int runMode             = 3;        // 0 = GenAlg, 1 = Player vs Bot, 2 = Bot vs Bot, 3 = Test all Cards

    // Used for Player vs Bot
    public static final String[] noEffDeck = new String[]{
            "LDG005","LDG006","LDG007","LDG014","LDG009","LDG010",
            "PFF006","OMA006","OMA008","OMA010","OMA011","OMA012",
            "OMA013","OWA001","OWA007","OWA010","LRE009","LMA002",};
    public static final String[] residentDeck = noEffDeck;
    public static final String[] opponentDeck = new String[]{
            "ACRR006","SFR003","EPP009","POM013","PFF001","PFF005",
            "PFF006","OMA006","OMA008","OMA010","OMA011","OMA012",
            "OMA013","OWA001","OWA007","OWA010","LRE009","LMA002",};

    public static void main(String[] args) {
        Main.random = new SecureRandom();

        if (doPreProcessing) {
            runPreProcessor();
        }

        if (runMode == 0) {
            runGenAlg(candidatesFile);

        } else if (runMode == 3) {
            createAllCardsCandiates();
            runGenAlg(allCardsTestFile);

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

    private static void runGenAlg(String canFile) {
        GenAlg genAlg = new GenAlg(cardsFile, rulesFile, residentsFile, canFile);
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