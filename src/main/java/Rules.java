import lombok.Getter;

import java.util.*;

public class Rules {

    Random random;
    @Getter private final int energyStarting;
    @Getter private final int energyMin;
    @Getter private final int energyMax;
    @Getter private final int energyPerTurn;
    private List<RoundBonus> guaranteedRoundBoni;
    private List<RoundBonus> additionalRoundBoni;
    @Getter private RoundBonus[] roundBoni;

    public Rules(int energyStarting, int energyMin, int energyMax, int energyPerTurn,
                 List<RoundBonus> guaranteedRoundBoni, List<RoundBonus> additionalRoundBoni, Random random) {
        this.random = random;

        this.energyStarting = energyStarting;
        this.energyMin = energyMin;
        this.energyMax = energyMax;
        this.energyPerTurn = energyPerTurn;

        this.guaranteedRoundBoni = guaranteedRoundBoni;
        this.additionalRoundBoni = additionalRoundBoni;
        this.roundBoni = new RoundBonus[5];
    }

    public void chooseRoundBoni() {
        List<Integer> toChooseRounds = new ArrayList<>(Arrays.asList(0,1,2,3,4));
        Collections.shuffle(toChooseRounds);

        for (RoundBonus roundBonus : this.guaranteedRoundBoni) {
            this.roundBoni[toChooseRounds.remove(0)] = roundBonus;
        }

        int firstAdditional = this.random.nextInt(3);
        this.roundBoni[toChooseRounds.remove(0)] = this.additionalRoundBoni.get(firstAdditional);

        int secondAdditional = this.random.nextInt(3);
        while (firstAdditional == secondAdditional) {
            secondAdditional = this.random.nextInt(3);
        }
        this.roundBoni[toChooseRounds.remove(0)] = this.additionalRoundBoni.get(secondAdditional);
    }
}
