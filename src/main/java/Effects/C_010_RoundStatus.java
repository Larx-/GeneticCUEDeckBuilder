package Effects;

import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_010_RoundStatus implements ConditionInterface {

    Game game;  // Following status has to be true
    int status; // 2 winning, 1 winning or tied, 0 tied, -1 loosing or tied, -2 loosing

    public C_010_RoundStatus(int status) {
        this.status = status;
    }

    public C_010_RoundStatus(int status, Game game, Boolean isOpponentView) {
        this.status = status;
        this.initialize(game, isOpponentView);
    }

    public void initialize(Game game, Boolean isOpponentView) {
        this.game = game;

        if (isOpponentView) {
            this.status = -this.status;
        }
    }

    @Override
    public boolean checkConditionFulfilled() {
        if (this.game == null) {
            log.error("Condition was not initialized!");
        }

        int powerBalance = this.game.getPowerBalance();

        // Tied
        if (powerBalance == 0) {
            return this.status == 1 || this.status == 0 || this.status == -1;

        // Winnning
        } else if (powerBalance > 0) {
            return this.status == 2 || this.status == 1;

        // Loosing
        } else {
            return this.status == -1 || this.status == -2;
        }
    }
}
