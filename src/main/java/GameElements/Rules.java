package GameElements;

import Controlling.Main;
import lombok.Getter;

import java.util.*;

public class Rules {

    @Getter private final int energyStarting;
    @Getter private final int energyMin;
    @Getter private final int energyMax;
    @Getter private final int energyPerTurn;
    private List<RoundBonus> guaranteedRoundBoni;
    private List<RoundBonus> additionalRoundBoni;
    private RoundBonus[] roundBoni;

    public Rules(int energyStarting, int energyMin, int energyMax, int energyPerTurn,
                 List<RoundBonus> guaranteedRoundBoni, List<RoundBonus> additionalRoundBoni) {

        this.energyStarting = energyStarting;
        this.energyMin = energyMin;
        this.energyMax = energyMax;
        this.energyPerTurn = energyPerTurn;

        this.guaranteedRoundBoni = guaranteedRoundBoni;
        this.additionalRoundBoni = additionalRoundBoni;
        this.roundBoni = new RoundBonus[5];
    }

    public RoundBonus getRoundBonus(int round) {
        return this.roundBoni[round-1];
    }

    public void chooseRoundBoni() {
        List<Integer> toChooseRounds = new ArrayList<>(Arrays.asList(0,1,2,3,4));
        Collections.shuffle(toChooseRounds);

        for (RoundBonus roundBonus : this.guaranteedRoundBoni) {
            this.roundBoni[toChooseRounds.remove(0)] = roundBonus;
        }

        int firstAdditional = Main.random.nextInt(3);
        this.roundBoni[toChooseRounds.remove(0)] = this.additionalRoundBoni.get(firstAdditional);

        int secondAdditional = Main.random.nextInt(3);
        while (firstAdditional == secondAdditional) {
            secondAdditional = Main.random.nextInt(3);
        }
        this.roundBoni[toChooseRounds.remove(0)] = this.additionalRoundBoni.get(secondAdditional);
    }
}
