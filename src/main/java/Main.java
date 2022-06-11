import java.util.*;

public class Main {

    public static void main(String[] args) {

        Random random = new Random(1);
        Controller controller = new Controller(random);

        // Initialize dictionary of prototype cards
        controller.initTestCards(30);

        Deck d1 = controller.createRandomDeck(18);
        Deck d2 = controller.createRandomDeck(18);

        AgentInterface a1 = new AgentRandom(d1,random);
        AgentInterface a2 = new AgentRandom(d2,random);
//        AgentInterface a2 = new AgentPlayer(d2);

        // TODO: Read rules from file or something
        List<RoundBonus> guaranteedRoundBoni = new ArrayList<>();
        guaranteedRoundBoni.add(new RoundBonus("AoC", "Norse Mythology", 70));
        guaranteedRoundBoni.add(new RoundBonus("AoC", "Something Else", 60));
        guaranteedRoundBoni.add(new RoundBonus("AoC", "Third one", 50));

        List<RoundBonus> additionalRoundBoni = new ArrayList<>();
        additionalRoundBoni.add(new RoundBonus("LOL", "Mammals", 40));
        additionalRoundBoni.add(new RoundBonus("LOL", "Optional", 30));
        additionalRoundBoni.add(new RoundBonus("LOL", "Last one", 20));

        Rules rules = new Rules(30, 0, 45, 15, guaranteedRoundBoni, additionalRoundBoni, random);
        Game game = new Game(rules,a1,a2);

        game.rules.chooseRoundBoni();
        game.playGame();

        System.out.println("\n");

        game.rules.chooseRoundBoni();
        game.playGame();
    }
}
