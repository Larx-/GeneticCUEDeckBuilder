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

    public boolean containsCard (String cardStr) {
        for (String cStr : deckStrArray) {
            if (cardStr.equals(cStr)) {
                return true;
            }
        }
        return false;
    }

    public String[] mutate(int mutationSpot, String cardStr) {
        String[] mutated = Arrays.copyOf(this.deckStrArray, this.deckStrArray.length);
        mutated[mutationSpot] = cardStr;
        return mutated;
    }
}
