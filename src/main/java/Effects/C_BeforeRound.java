package Effects;

import Enums.Who;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_BeforeRound extends Condition {

    int beforeRound;

    public C_BeforeRound(int beforeRound) {
        this.beforeRound = beforeRound;
    }

    @Override
    public boolean checkConditionFulfilled (Game game, Who selfPlayer) {
        return this.beforeRound > game.getRoundNumber();
    }
}
