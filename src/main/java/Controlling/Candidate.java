package Controlling;

import Agents.AgentInterface;
import Agents.AgentRandom;
import GameElements.Deck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Candidate {

    AgentInterface agent;
    List<Float> results;
    float fitness;
    String[] deckStrArray;

    public Candidate(Deck deck) {
        this.agent = new AgentRandom(deck);
        this.results = new ArrayList<>();
        this.fitness = 0.0f;
        this.deckStrArray = deck.toStringArray();
    }

    public float addResults() {
        float rc = 0;
        for (Float f : results) {
            rc += f;
        }
        return rc;
    }

    public String[] mutate(int mutationSpot, String cardStr) {
        if (!Arrays.asList(deckStrArray).contains(cardStr)) {
            String[] mutated = Arrays.copyOf(this.deckStrArray, this.deckStrArray.length);
            mutated[mutationSpot] = cardStr;
            return mutated;
        }
        return this.deckStrArray;
    }
}
