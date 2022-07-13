package Effects;

import Enums.Who;
import GameElements.Game;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class C_RoundsLost extends Condition {

    String sign;
    int rounds;

    public C_RoundsLost(String signedRounds) {
        this.sign = "==";

        if (signedRounds.charAt(0) == '>' || signedRounds.charAt(0) == '<') {
            if (signedRounds.charAt(1) == '=') {
                this.sign = signedRounds.substring(0,2);
                signedRounds = signedRounds.substring(2);

            } else {
                this.sign = signedRounds.substring(0,1);
                signedRounds = signedRounds.substring(1);
            }
        }

        this.rounds = Integer.parseInt(signedRounds);
    }

    @Override
    public boolean checkConditionFulfilled(Game game, Who selfPlayer) {
        int losses = selfPlayer == Who.RESIDENT ? game.getOppWins() : game.getResWins();

        switch (this.sign) {
            case ">":  return losses >  this.rounds;
            case ">=": return losses >= this.rounds;
            case "<=": return losses <= this.rounds;
            case "<":  return losses <  this.rounds;
            case "==":
            default:   return losses == this.rounds;
        }
    }
}