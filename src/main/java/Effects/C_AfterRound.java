package Effects;

import Enums.Who;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_AfterRound extends Condition {

    int afterRound;

    public C_AfterRound(int afterRound) {
        this.afterRound = afterRound;
    }

    @Override
    public boolean checkConditionFulfilled(Game game, Who selfPlayer) {
        return this.afterRound < game.getRoundNumber();
    }
}