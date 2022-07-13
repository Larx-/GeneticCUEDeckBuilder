package Effects;

import Enums.Who;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_TurnState extends Condition {

    String state;

    public C_TurnState(String state) {
        this.state = state;
    }

    @Override
    public boolean checkConditionFulfilled (Game game, Who selfPlayer) {
        int power = game.getLastPowerDiff(); // Positive means resident is winning
        
        // Assuming view from resident, so flip power if view from opponent
        if (selfPlayer == Who.OPPONENT) {
            power = -power;
        }
        
        switch (this.state) {
            case "Win":    if (power > 0)  { return true; } break;
            case "EqWin":  if (power >= 0) { return true; } break;
            case "Equal":  if (power == 0) { return true; } break;
            case "EqLoss": if (power <= 0) { return true; } break;
            case "Loss":   if (power < 0)  { return true; } break;
        }
        return false;
    }
}
