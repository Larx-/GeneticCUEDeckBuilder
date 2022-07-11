package Controlling;

import Agents.AgentInterface;

import java.util.ArrayList;
import java.util.List;

public class Candidate {

    AgentInterface agent;
    List<Float> results;
    float fitness;

    public Candidate(AgentInterface agent) {
        this.agent = agent;
        this.results = new ArrayList<>();
        this.fitness = 0.0f;
    }

    public float addResults() {
        float rc = 0;
        for (Float f : results) {
            rc += f;
        }
        return rc;
    }
}
