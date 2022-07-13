package EffectConditions;

import Enums.Who;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_TurnInRound extends Condition {

    int turn;

    public C_TurnInRound(int turn) {
        this.turn = turn;
    }

    @Override
    public boolean checkConditionFulfilled(Game game, Who selfPlayer) {
        return this.turn == game.getTurnNumber();
    }
}