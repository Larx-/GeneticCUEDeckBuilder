package Controlling;

import Agents.*;
import GameElements.*;
import Setup.*;
import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;

@Log4j2
public class Main {

    public static SecureRandom random;
    public static String SEPARATOR = ",";
    public static String NEW_LINE  = "\n";

    public static void main(String[] args) {

        Main.random = new SecureRandom(new byte[]{1,1,1,0,0,0,1,1,1,0,0,0});
        DeckInitializer deckInitializer = new DeckInitializer();

        // Initialize dictionary of prototype cards
        deckInitializer.initTestCards(30);

        Deck d1 = deckInitializer.createRandomDeck(18);
        Deck d2 = deckInitializer.createRandomDeck(18);

//        AgentInterface a1 = new AgentRandom(d1);
//        AgentInterface a2 = new AgentRandom(d2);
        AgentInterface a1 = new AgentPlayer(d1);
        AgentInterface a2 = new AgentPlayer(d2);

        Rules rules = RulesInitializer.getTestRules();
        Game game = new Game(rules,a1,a2);

        game.playGame();

//        System.out.println("\n");

        // FIXME: Cards not resetting after game
//        game.playGame();
    }
}