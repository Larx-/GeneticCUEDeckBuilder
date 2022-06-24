package Effects;

import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_001_AfterRoundX implements ConditionInterface {

    Game game;
    int afterRound;

    public C_001_AfterRoundX(int afterRound) {
        this.afterRound = afterRound;
    }

    public C_001_AfterRoundX(int afterRound, Game game) {
        this.afterRound = afterRound;
        this.initialize(game);
    }

    public void initialize(Game game) {
        this.game = game;
    }

    @Override
    public boolean checkConditionFulfilled() {
        if (this.game == null) {
            log.error("Condition was not initialized!");
        }

        return this.afterRound < this.game.getRoundNumber();
    }
}
