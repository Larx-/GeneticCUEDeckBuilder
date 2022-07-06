package Controlling;

import Agents.*;
import GameElements.*;
import Setup.*;
import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;

@Log4j2
public class Main {

    public static SecureRandom random;

    public static void main(String[] args) {

        Main.random = new SecureRandom();
        DeckInitializer deckInitializer = new DeckInitializer("src/main/resources/Cards/cards.csv");
        RulesInitializer rulesInitializer = new RulesInitializer();

        Rules rules = rulesInitializer.getRulesFromFile("src/main/resources/Rules/rules_1.json");

        Deck d1 = deckInitializer.createRandomDeck();
        Deck d2 = deckInitializer.createRandomDeck();

        AgentInterface a1 = new AgentRandom(d1);
        AgentInterface a2 = new AgentRandom(d2);
//        AgentInterface a1 = new AgentPlayer(d1);
//        AgentInterface a2 = new AgentPlayer(d2);

        Game game = new Game(rules,a1,a2);

        game.playGame();
        game.playGame();
    }
}