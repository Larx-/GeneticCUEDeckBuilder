package EffectConditions;

import Enums.Who;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Condition {

    public boolean checkConditionFulfilled(Game game, Who selfPlayer){
        log.error("Condition check went wrong");
        return false;
    }
}