package EffectConditions;

import Enums.Who;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_AfterTurn extends Condition {

    int afterTotalTurn;

    public C_AfterTurn(int afterTotalTurn) {
        this.afterTotalTurn = afterTotalTurn;
    }

    @Override
    public boolean checkConditionFulfilled(Game game, Who selfPlayer) {
        return this.afterTotalTurn < game.getTotalTurnNumber();
    }
}