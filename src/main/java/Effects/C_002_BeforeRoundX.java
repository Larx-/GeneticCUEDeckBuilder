package Effects;

import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_002_BeforeRoundX implements ConditionInterface {
    
    Game game;
    int beforeRound;

    public C_002_BeforeRoundX(int afterRound) {
        this.beforeRound = afterRound;
    }

    public C_002_BeforeRoundX(int afterRound, Game game) {
        this.beforeRound = afterRound;
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

        return this.beforeRound > this.game.getRoundNumber();
    }
}
