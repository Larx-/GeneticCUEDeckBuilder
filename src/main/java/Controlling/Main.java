package Controlling;

import Agents.AgentInterface;
import Agents.AgentPlayer;
import Agents.AgentRandom;
import GameElements.Deck;
import GameElements.Game;
import GameElements.Rules;
import Setup.DeckInitializer;
import Setup.RulesInitializer;
import Setup.TSVtoCSVPreProcessor;
import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;
import java.util.Scanner;

@Log4j2
public class Main {

    public static int numErr = 0;

    public static SecureRandom random;
    public static final String cardsFile = "src/main/resources/Cards/cards.csv";
    public static final String rulesFile = "src/main/resources/Rules/rules_1.json";
    public static final String residentsFile = "src/main/resources/Residents/residents.csv";
    public static final String candidatesFile = null;

    public static final String resultsDir = "src/main/resources/Results";
    public static final String resultsName = "Test_1";

    public static final boolean doPreProcessing = true;
    public static final boolean doPreDefDecks   = false;
    public static final boolean doEndlessMode   = true;     // Not available for GenAlg mode
    public static final int runMode             = 2;        // 0 = GenAlg, 1 = Player vs Bot, 2 = Bot vs Bot

    // TODO: Bugs in the simulation part of the software:
    //       SNB023]  Medusa Nebula -> Does not seem to give a random card of the opponent -36

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
            runGenAlg();

        } else {
            if (doEndlessMode) { loopGames(); }
            else               { runGame();   }
        }
    }

    private static void runPreProcessor() {
        TSVtoCSVPreProcessor preProcessor = new TSVtoCSVPreProcessor();
        preProcessor.processTSVtoCSV();
    }

    private static void runGenAlg() {
        GenAlg genAlg = new GenAlg(cardsFile, rulesFile, residentsFile, candidatesFile);
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