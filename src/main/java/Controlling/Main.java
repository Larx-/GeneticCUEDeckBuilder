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

    public static final int runMode = -2; // -2 = Endless Player vs Bot,  -1 = Player vs Bot,  0 = Only GenAlg,  1 = Only TSVtoCSVPreProcessor,  2 = Both

    // Used for Player vs Bot
    public static final String[] noEffDeck = new String[]{
            "LDG005","LDG006","LDG007","LDG014","LDG009","LDG010",
            "PFF006","OMA006","OMA008","OMA010","OMA011","OMA012",
            "OMA013","OWA001","OWA007","OWA010","LRE009","LMA002",};
    public static final String[] botDeck = noEffDeck;
    public static final String[] playerDeck = new String[]{
            "ACRR006","SFR003","EPP009","POM013","PFF001","PFF005",
            "PFF006","OMA006","OMA008","OMA010","OMA011","OMA012",
            "OMA013","OWA001","OWA007","OWA010","LRE009","LMA002",};

    public static void main(String[] args) {
        Main.random = new SecureRandom();

        switch (runMode) {
            case -2:
                loopPlayerGames();
                break;
            case -1:
                runPlayerGame();
                break;
            case 0:
                runGenAlg();
                break;
            case 1:
                runPreProcessor();
                break;
            case 2:
                runPreProcessor();
                runGenAlg();
                break;
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

    private static void loopPlayerGames() {
        Scanner scanner = new Scanner(System.in);
        String inputLine = "y";

        while (inputLine.equalsIgnoreCase("y")) {
            runPlayerGame();

            log.debug("Play another game? [Y/n]");
            inputLine = scanner.nextLine();

            if (inputLine.equals("")) {
                inputLine = "y";
            }
        }
    }

    private static void runPlayerGame() {
        DeckInitializer deckInitializer = new DeckInitializer(cardsFile);
        RulesInitializer rulesInitializer = new RulesInitializer();
        Rules rules = rulesInitializer.getRulesFromFile(rulesFile);

        Deck d1 = deckInitializer.createRandomDeckWithCardList(botDeck);
        Deck d2 = deckInitializer.createRandomDeckWithCardList(playerDeck);

        AgentInterface a1 = new AgentRandom(d1);
        AgentInterface a2 = new AgentPlayer(d2);

        Game game = new Game(rules, a1, a2);
        game.setDoOutput(true);
        game.playGame();
    }
}