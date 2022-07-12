package Controlling;

import Agents.AgentInterface;
import Agents.AgentRandom;
import GameElements.Deck;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Candidate {

    @Getter @Setter float fitness;
    @Getter String[] deckStrArray;

    public Candidate(String[] deckStrArray) {
        this.fitness = 0.0f;
        this.deckStrArray = deckStrArray;
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
